package com.ep.mqtt.server.deal;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.db.dao.*;
import com.ep.mqtt.server.db.dto.*;
import com.ep.mqtt.server.job.AsyncJobManage;
import com.ep.mqtt.server.job.DispatchMessageParam;
import com.ep.mqtt.server.metadata.*;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.ep.mqtt.server.raft.transfer.AddTopicFilter;
import com.ep.mqtt.server.raft.transfer.TransferData;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.util.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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

    @Transactional(rollbackFor = Exception.class)
    public void clearClientData(String clientId) {
        clientDao.deleteByClientId(clientId);
        clientSubscribeDao.deleteByClientId(clientId);
        receiveQos2MessageDao.deleteByFromClientId(clientId);
        sendMessageDao.deleteByToClientId(clientId);
        messageIdProgressDao.deleteByClientId(clientId);
    }

    /**
     * 处理订阅报文（暂不支持保留消息）
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
        Set<String> editTopicFilterSet = Sets.newHashSet();
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
                    editTopicFilterSet.add(existClientSubscribeDto.getTopicFilter());
                }

                continue;
            }

            ClientSubscribeDto insert = new ClientSubscribeDto();
            insert.setQos(subscribeQos);
            insert.setClientId(clientId);
            insert.setSubscribeTime(now.getTime());
            insert.setTopicFilter(clientSubscribe.topicName());

            editClientSubscribeDtoList.add(insert);
            editTopicFilterSet.add(clientSubscribe.topicName());
        }

        if (!CollectionUtils.isEmpty(editTopicFilterSet)){
            clientSubscribeDao.insertOrUpdate(editClientSubscribeDtoList);

            AddTopicFilter addTopicFilter = new AddTopicFilter();
            addTopicFilter.setTopicFilterSet(editTopicFilterSet);

            EasyMqttRaftClient.syncSend(JsonUtil.obj2String(
                    new TransferData(RaftCommand.ADD_TOPIC_FILTER, JsonUtil.obj2String(addTopicFilter))));
        }

        MqttUtil.sendSubAck(channelHandlerContext, subMessageId, qoses);
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
                clearClientData(clientId);
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
            clearClientData(clientId);
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

        DispatchMessageParam dispatchMessageParam = new DispatchMessageParam();
        dispatchMessageParam.setReceiveQos(receiveQos);
        dispatchMessageParam.setTopic(topic);
        dispatchMessageParam.setReceivePacketId(receivePacketId);
        dispatchMessageParam.setFromClientId(fromClientId);
        dispatchMessageParam.setPayload(payload);
        asyncJobManage.addJob(AsyncJobBusinessType.DISPATCH_MESSAGE.getBusinessId(UUID.randomUUID().toString()),
            AsyncJobBusinessType.DISPATCH_MESSAGE, dispatchMessageParam, now);

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
                DispatchMessageParam dispatchMessageParam = new DispatchMessageParam();
                dispatchMessageParam.setReceiveQos(receiveQos2MessageDto.getReceiveQos());
                dispatchMessageParam.setTopic(receiveQos2MessageDto.getTopic());
                dispatchMessageParam.setReceivePacketId(receiveQos2MessageDto.getReceivePacketId());
                dispatchMessageParam.setFromClientId(receiveQos2MessageDto.getFromClientId());
                dispatchMessageParam.setPayload(receiveQos2MessageDto.getPayload());
                asyncJobManage.addJob(AsyncJobBusinessType.DISPATCH_MESSAGE.getBusinessId(UUID.randomUUID().toString()),
                    AsyncJobBusinessType.DISPATCH_MESSAGE, dispatchMessageParam, new Date());
            }
        }

        MqttUtil.sendPubComp(channelHandlerContext, receivePacketId);
    }

    public void disConnect(ChannelHandlerContext channelHandlerContext){
        NettyUtil.setCleanDataReason(channelHandlerContext, "disConnect");

        channelHandlerContext.close();
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
