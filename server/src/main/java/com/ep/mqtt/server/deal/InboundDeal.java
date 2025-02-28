package com.ep.mqtt.server.deal;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.db.dao.*;
import com.ep.mqtt.server.db.dto.ClientDto;
import com.ep.mqtt.server.db.dto.ClientSubscribeDto;
import com.ep.mqtt.server.db.dto.MessageIdProgressDto;
import com.ep.mqtt.server.db.dto.ReceiveQos2MessageDto;
import com.ep.mqtt.server.job.AsyncJobManage;
import com.ep.mqtt.server.job.DispatchMessageParam;
import com.ep.mqtt.server.metadata.*;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.ep.mqtt.server.raft.transfer.AddTopicFilter;
import com.ep.mqtt.server.raft.transfer.TransferData;
import com.ep.mqtt.server.util.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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

    private Integer genMessageId(String clientId) {
        String genMessageIdKey = StoreKey.GEN_MESSAGE_ID_KEY.formatKey(clientId);
        RedisScript<Long> redisScript = new DefaultRedisScript<>(LuaScript.GEN_MESSAGE_ID, Long.class);
        Long messageId = stringRedisTemplate.execute(redisScript, Lists.newArrayList(genMessageIdKey));
        if (messageId != null) {
            return Math.toIntExact(messageId % 65535 + 1);
        }
        return null;
    }

    public void delMessage(String clientId, Integer messageId) {
        stringRedisTemplate.opsForHash().delete(StoreKey.MESSAGE_KEY.formatKey(clientId), String.valueOf(messageId));
    }

    public void saveRelMessage(String clientId, Integer messageId) {
        String relMessageKey = StoreKey.REL_MESSAGE_KEY.formatKey(clientId);
        RedisScript<Long> redisScript = new DefaultRedisScript<>(LuaScript.SAVE_REL_MESSAGE);
        stringRedisTemplate.execute(redisScript, Lists.newArrayList(relMessageKey), String.valueOf(messageId));
    }

    public void delRelMessage(String clientId, Integer messageId) {
        stringRedisTemplate.opsForSet().remove(StoreKey.REL_MESSAGE_KEY.formatKey(clientId), String.valueOf(messageId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void connect(String clientId, boolean isCleanSession) {
        ClientDto existClientDto = clientDao.selectByClientId(clientId);

        if (isCleanSession) {
            if (existClientDto != null) {
                // 清除之前的数据
                clearClientData(clientId);
            }

            saveClientInfo(clientId, true);
            return;
        }

        if (existClientDto == null) {
            saveClientInfo(clientId, false);
            return;
        }

        // 之前有会话，但之前的会话的设置的不持久化数据，所以清理之前的数据
        if (YesOrNo.YES.equals(existClientDto.getIsCleanSession())) {
            clearClientData(clientId);
            saveClientInfo(clientId, false);
            return;
        }

        // 更新客户端连接时间
        clientDao.updateConnectTime(clientId, System.currentTimeMillis());

        // 重发消息
        WorkerThreadPool.execute((a) -> {
            // TODO: 2025/1/24 这里等publish报文完成再实现，以下逻辑仅供参考
            // 查询qos=1、未puback发送消息记录，重试推送publish报文任务

            // 查询qos=2、未comp发送该消息记录，其中未rec的重试推送publish报文任务，已rec需要重试推送pubrel报文任务

            // 这里重试任务，即根据任务id更新任务状态为READY的任务执行时间

        });
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
