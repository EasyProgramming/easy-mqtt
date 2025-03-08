package com.ep.mqtt.server.deal;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.db.dao.*;
import com.ep.mqtt.server.db.dto.*;
import com.ep.mqtt.server.job.AsyncJobManage;
import com.ep.mqtt.server.metadata.*;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.ep.mqtt.server.raft.transfer.AddRetainMessage;
import com.ep.mqtt.server.raft.transfer.AddTopicFilter;
import com.ep.mqtt.server.raft.transfer.RemoveRetainMessage;
import com.ep.mqtt.server.raft.transfer.TransferData;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.store.RetainMessageStore;
import com.ep.mqtt.server.util.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 入站报文处理器
 * 
 * @author zbz
 * @date 2023/7/15 17:10
 */
@Slf4j
@Component
public class InboundDeal {

    private final static ThreadPoolExecutor RETRY_SEND_MESSAGE_THREAD_POOL =
        new ThreadPoolExecutor(Constant.PROCESSOR_NUM * 2, Constant.PROCESSOR_NUM * 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("retry-send-message-%s").build());

    @Resource
    private MqttServerProperties mqttServerProperties;

    @Resource
    private ClientDao clientDao;

    @Resource
    private ClientSubscribeDao clientSubscribeDao;

    @Resource
    private ReceiveQos2MessageDao receiveQos2MessageDao;

    @Resource
    private SendMessageDao sendMessageDao;

    @Resource
    private AsyncJobManage asyncJobManage;

    @Resource
    private MessageIdProgressDao messageIdProgressDao;

    @Resource
    private TransactionUtil transactionUtil;

    @Resource
    private CommonDeal clearClientData;

    public boolean authentication(MqttConnectMessage mqttConnectMessage) {
        if (StringUtils.isBlank(mqttServerProperties.getAuthenticationUrl())) {
            return true;
        }
        String userName = mqttConnectMessage.payload().userName();
        byte[] password = mqttConnectMessage.payload().passwordInBytes();
        String returnStr = HttpUtil.getInstance().postJson(mqttServerProperties.getAuthenticationUrl(),
            JsonUtil.obj2String(new AuthenticationRequest(userName, new String(password))), null);
        return Boolean.parseBoolean(returnStr);
    }

    public void retrySendMessage(String clientId) {
        RETRY_SEND_MESSAGE_THREAD_POOL.submit(() -> {
            try {
                transactionUtil.transaction(() -> {
                    List<SendMessageDto> selectRetryMessageDtoList = sendMessageDao.selectRetryMessage(clientId);
                    if (CollectionUtils.isEmpty(selectRetryMessageDtoList)) {
                        return null;
                    }

                    for (SendMessageDto selectRetryMessageDto : selectRetryMessageDtoList) {
                        Session session = SessionManager.get(clientId);
                        if (session == null) {
                            return null;
                        }

                        if (selectRetryMessageDto.getSendQos() == Qos.LEVEL_1) {
                            MqttUtil.sendPublish(session.getChannelHandlerContext(), true, selectRetryMessageDto.getSendQos(),
                                selectRetryMessageDto.getIsRetain().getBoolean(), selectRetryMessageDto.getTopic(),
                                selectRetryMessageDto.getSendPacketId(), selectRetryMessageDto.getPayload());
                        } else if (selectRetryMessageDto.getSendQos() == Qos.LEVEL_2) {
                            if (selectRetryMessageDto.getIsReceivePubRec().getBoolean()) {
                                MqttUtil.sendPubRel(session.getChannelHandlerContext(), selectRetryMessageDto.getSendPacketId());
                            } else {
                                MqttUtil.sendPublish(session.getChannelHandlerContext(), true, selectRetryMessageDto.getSendQos(),
                                    selectRetryMessageDto.getIsRetain().getBoolean(), selectRetryMessageDto.getTopic(),
                                    selectRetryMessageDto.getSendPacketId(), selectRetryMessageDto.getPayload());
                            }
                        } else {
                            log.warn("不应存在的数据,[{}]", JsonUtil.obj2String(selectRetryMessageDto));
                        }
                    }

                    return null;
                });
            } catch (Throwable e) {
                log.error("客户端id：[{}]，重发消息异常", clientId, e);
            }
        });
    }

    /**
     * 处理订阅报文
     * @param channelHandlerContext netty上下文
     * @param subMessageId 订阅报文的消息id
     * @param clientId 客户端id
     * @param clientSubscribeList 订阅的topic filter列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void subscribe(ChannelHandlerContext channelHandlerContext, int subMessageId, String clientId,
                          List<MqttTopicSubscription> clientSubscribeList) {
        Date now = new Date();

        List<ClientSubscribeDto> existClientSubscribeList = clientSubscribeDao.getClientSubscribe(clientId,
                clientSubscribeList.stream().map(MqttTopicSubscription::topicName).collect(Collectors.toSet()));

        Map<String, ClientSubscribeDto> existClientSubscribeMap =
                existClientSubscribeList.stream().collect(Collectors.toMap(ClientSubscribeDto::getTopicFilter, b -> b));

        int index = 0;
        MqttQoS[] qoses = new MqttQoS[clientSubscribeList.size()];
        Map<String, Qos> editTopicFilterMap = Maps.newHashMap();
        List<ClientSubscribeDto> editClientSubscribeDtoList = Lists.newArrayList();
        for (MqttTopicSubscription clientSubscribe : clientSubscribeList){
            qoses[index] = clientSubscribe.qualityOfService();
            index++;
            Qos subscribeQos = BaseEnum.getByCode(clientSubscribe.qualityOfService().value(), Qos.class);

            ClientSubscribeDto existClientSubscribeDto = existClientSubscribeMap.get(clientSubscribe.topicName());
            if (existClientSubscribeDto != null){
                if (existClientSubscribeDto.getQos() != subscribeQos){
                    ClientSubscribeDto update = new ClientSubscribeDto();
                    update.setId(existClientSubscribeDto.getId());
                    update.setQos(subscribeQos);
                    update.setSubscribeTime(now.getTime());

                    editClientSubscribeDtoList.add(update);
                    editTopicFilterMap.put(existClientSubscribeDto.getTopicFilter(), subscribeQos);
                }

                continue;
            }

            ClientSubscribeDto insert = new ClientSubscribeDto();
            insert.setQos(subscribeQos);
            insert.setClientId(clientId);
            insert.setSubscribeTime(now.getTime());
            insert.setTopicFilter(clientSubscribe.topicName());

            editClientSubscribeDtoList.add(insert);
            editTopicFilterMap.put(clientSubscribe.topicName(), subscribeQos);
        }

        if (!CollectionUtils.isEmpty(editTopicFilterMap)){
            clientSubscribeDao.insertOrUpdate(editClientSubscribeDtoList);

            dealRetainMessage(clientId, now, editTopicFilterMap);

            AddTopicFilter addTopicFilter = new AddTopicFilter();
            addTopicFilter.setTopicFilterSet(editTopicFilterMap.keySet());
            EasyMqttRaftClient.syncSend(JsonUtil.obj2String(
                    new TransferData(RaftCommand.ADD_TOPIC_FILTER, JsonUtil.obj2String(addTopicFilter))));
        }

        MqttUtil.sendSubAck(channelHandlerContext, subMessageId, qoses);
    }

    private void dealRetainMessage(String clientId, Date now, Map<String, Qos> editTopicFilterMap){
        Long validTime = now.getTime() + 1000L * 60 * 60 * 24 * 7;
        List<SendMessageDto> qos0MessageDtoList = Lists.newArrayList();
        List<SendMessageDto> otherMessageDtoList = Lists.newArrayList();
        for (Map.Entry<String, Qos> entry : editTopicFilterMap.entrySet()) {
            List<RetainMessageStore.RetainMessage> retainMessageList = RetainMessageStore.matchRetainMessage(entry.getKey());
            if (CollectionUtils.isEmpty(retainMessageList)){
                continue;
            }

            for (RetainMessageStore.RetainMessage retainMessage : retainMessageList){
                SendMessageDto sendMessageDto = ModelUtil.buildSendMessageDto(
                        retainMessage.getReceiveQos(),
                        retainMessage.getReceivePacketId(),
                        retainMessage.getFromClientId(),
                        entry.getValue().getCode() >= retainMessage.getReceiveQos().getCode() ? retainMessage.getReceiveQos() : entry.getValue(),
                        retainMessage.getTopic(),
                        null,
                        clientId,
                        retainMessage.getPayload(),
                        YesOrNo.NO,
                        validTime,
                        YesOrNo.YES
                );

                if (sendMessageDto.getSendQos() == Qos.LEVEL_0) {
                    qos0MessageDtoList.add(sendMessageDto);
                } else {
                    otherMessageDtoList.add(sendMessageDto);
                }
            }
        }

        if (!CollectionUtils.isEmpty(otherMessageDtoList)){
            sendMessageDao.insert(otherMessageDtoList, 10000);
        }

        List<AsyncJobDto> genMessageIdAsyncJobDtoList = Lists.newArrayList();
        for (SendMessageDto sendMessageDto : qos0MessageDtoList) {
            genMessageIdAsyncJobDtoList.add(ModelUtil.buildGenMessageIdAsyncJobDto(sendMessageDto, now.getTime()));
        }
        for (SendMessageDto sendMessageDto : otherMessageDtoList) {
            genMessageIdAsyncJobDtoList.add(ModelUtil.buildGenMessageIdAsyncJobDto(sendMessageDto, now.getTime()));
        }

        if (!CollectionUtils.isEmpty(genMessageIdAsyncJobDtoList)){
            asyncJobManage.addJob(genMessageIdAsyncJobDtoList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unSubscribe(ChannelHandlerContext channelHandlerContext, int unSubMessageId, String clientId, Set<String> topicFilterSet) {
        clientSubscribeDao.deleteClientSubscribe(clientId, topicFilterSet);

        MqttUtil.sendUnSubAck(channelHandlerContext, unSubMessageId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void pubAck(String clientId, Integer messageId) {
        sendMessageDao.deleteAtLeastOnceMessage(clientId, messageId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void pubRec(ChannelHandlerContext channelHandlerContext, String clientId, Integer messageId) {
        sendMessageDao.updateReceivePubRec(clientId, messageId);

        MqttUtil.sendPubRel(channelHandlerContext, messageId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean connect(String clientId, boolean isCleanSession) {
        ClientDto existClientDto = clientDao.selectByClientId(clientId);

        if (isCleanSession) {
            if (existClientDto != null) {
                // 清除之前的数据
                clearClientData.clearClientData(clientId);
            }

            saveClientInfo(clientId, true);
            return false;
        }

        if (existClientDto == null) {
            saveClientInfo(clientId, false);
            return false;
        }

        // 之前有会话，但之前的会话的设置的不持久化数据，所以清理之前的数据
        if (YesOrNo.YES.equals(existClientDto.getIsCleanSession())) {
            clearClientData.clearClientData(clientId);
            saveClientInfo(clientId, false);
            return false;
        }

        // 更新客户端连接时间
        clientDao.updateConnectTime(clientId, System.currentTimeMillis());

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(ChannelHandlerContext channelHandlerContext, Qos receiveQos, String topic, Integer receivePacketId, String fromClientId,
        String payload, boolean isRetain) {
        if (receiveQos == null) {
            return;
        }

        if (isRetain){
            if (StringUtils.isBlank(payload)){
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        RemoveRetainMessage removeRetainMessage = new RemoveRetainMessage();
                        removeRetainMessage.setTopic(topic);

                        EasyMqttRaftClient.syncSend(JsonUtil.obj2String(
                                new TransferData(RaftCommand.REMOVE_RETAIN_MESSAGE, JsonUtil.obj2String(removeRetainMessage))));
                    }
                });
            }
            else {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        AddRetainMessage addRetainMessage = new AddRetainMessage();
                        addRetainMessage.setFromClientId(fromClientId);
                        addRetainMessage.setPayload(payload);
                        addRetainMessage.setReceivePacketId(receivePacketId);
                        addRetainMessage.setReceiveQos(receiveQos);
                        addRetainMessage.setTopic(topic);

                        EasyMqttRaftClient.syncSend(JsonUtil.obj2String(
                                new TransferData(RaftCommand.ADD_RETAIN_MESSAGE, JsonUtil.obj2String(addRetainMessage))));
                    }
                });
            }
        }

        Date now = new Date();

        if (Qos.LEVEL_2 == receiveQos) {
            try {
                ReceiveQos2MessageDto receiveQos2MessageDto = new ReceiveQos2MessageDto();
                receiveQos2MessageDto.setReceiveQos(receiveQos);
                receiveQos2MessageDto.setTopic(topic);
                receiveQos2MessageDto.setReceivePacketId(receivePacketId);
                receiveQos2MessageDto.setFromClientId(fromClientId);
                receiveQos2MessageDto.setPayload(payload);
                receiveQos2MessageDto.setReceiveTime(now.getTime());

                receiveQos2MessageDao.insert(receiveQos2MessageDto);
            } catch (DuplicateKeyException e) {
                log.warn("重复的消息 fromClientId:[{}], receivePacketId:[{}]", fromClientId, receivePacketId);
            }

            MqttUtil.sendPubRec(channelHandlerContext, receivePacketId);
            return;
        }

        asyncJobManage.addJob(AsyncJobBusinessType.DISPATCH_MESSAGE.getBusinessId(UUID.randomUUID().toString()),
            AsyncJobBusinessType.DISPATCH_MESSAGE, ModelUtil.buildDispatchMessageParam(receiveQos, topic, receivePacketId, fromClientId, payload),
                now);

        if (Qos.LEVEL_0 == receiveQos) {
            return;
        }

        MqttUtil.sendPubAck(channelHandlerContext, receivePacketId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void pubRel(ChannelHandlerContext channelHandlerContext, Integer receivePacketId, String fromClientId) {
        ReceiveQos2MessageDto receiveQos2MessageDto = receiveQos2MessageDao.selectByFromClientIdAndReceivePacketId(fromClientId, receivePacketId);
        if (receiveQos2MessageDto != null) {
            if (receiveQos2MessageDao.deleteByFromClientIdAndReceivePacketId(fromClientId, receivePacketId)) {
                asyncJobManage.addJob(AsyncJobBusinessType.DISPATCH_MESSAGE.getBusinessId(UUID.randomUUID().toString()),
                    AsyncJobBusinessType.DISPATCH_MESSAGE, ModelUtil.buildDispatchMessageParam(receiveQos2MessageDto.getReceiveQos(),
                                receiveQos2MessageDto.getTopic(), receiveQos2MessageDto.getReceivePacketId(),
                                receiveQos2MessageDto.getFromClientId(), receiveQos2MessageDto.getPayload()), new Date());
            }
        }

        MqttUtil.sendPubComp(channelHandlerContext, receivePacketId);
    }

    public void disConnect(ChannelHandlerContext channelHandlerContext){
        NettyUtil.setDisconnectReason(channelHandlerContext, DisconnectReason.NORMAL);

        channelHandlerContext.close();
    }

    @Transactional(rollbackFor = Exception.class)
    public void pubComp(String clientId, Integer messageId) {
        sendMessageDao.deleteExactlyOnceMessage(clientId, messageId);
    }

    private void saveClientInfo(String clientId, boolean isCleanSession) {
        ClientDto clientDto = new ClientDto();
        clientDto.setClientId(clientId);
        clientDto.setIsCleanSession(isCleanSession ? YesOrNo.YES : YesOrNo.NO);

        Long now = System.currentTimeMillis();
        clientDto.setCreateTime(now);
        clientDto.setLastConnectTime(now);

        clientDao.insert(clientDto);

        MessageIdProgressDto messageIdProgressDto = new MessageIdProgressDto();
        messageIdProgressDto.setClientId(clientId);
        messageIdProgressDto.setProgress(0L);

        messageIdProgressDao.insert(messageIdProgressDto);
    }

    @Data
    public static class AuthenticationRequest {

        private String username;

        private String password;

        public AuthenticationRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

}
