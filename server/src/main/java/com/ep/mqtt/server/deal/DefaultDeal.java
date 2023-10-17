package com.ep.mqtt.server.deal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.listener.msg.CleanExistSessionMsg;
import com.ep.mqtt.server.listener.msg.ManageRetainMessageMsg;
import com.ep.mqtt.server.listener.msg.ManageTopicFilterMsg;
import com.ep.mqtt.server.metadata.ChannelKey;
import com.ep.mqtt.server.metadata.LuaScript;
import com.ep.mqtt.server.metadata.StoreKey;
import com.ep.mqtt.server.metadata.YesOrNo;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.store.RetainMessageStore;
import com.ep.mqtt.server.store.SubscribeStore;
import com.ep.mqtt.server.util.*;
import com.ep.mqtt.server.vo.ClientInfoVo;
import com.ep.mqtt.server.vo.MessageVo;
import com.ep.mqtt.server.vo.TopicVo;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 请求broker
 * 
 * @author zbz
 * @date 2023/7/15 17:10
 */
@Slf4j
@Component
public class DefaultDeal {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SubscribeStore subscribeStore;

    @Autowired
    private MqttServerProperties mqttServerProperties;

    @Autowired
    private RetainMessageStore retainMessageStore;

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

    public void cleanExistSession(String clientId, String sessionId) {
        Session session = SessionManager.get(clientId);
        if (session != null && !session.getSessionId().equals(sessionId)) {
            session.getChannelHandlerContext().disconnect();
        }
        CleanExistSessionMsg cleanExistSessionMsg = new CleanExistSessionMsg();
        cleanExistSessionMsg.setClientId(clientId);
        cleanExistSessionMsg.setSessionId(sessionId);
        stringRedisTemplate.convertAndSend(ChannelKey.CLEAR_EXIST_SESSION.getKey(),
            JsonUtil.obj2String(cleanExistSessionMsg));
    }

    public ClientInfoVo getClientInfo(String clientId) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        String clientJsonStr = hashOperations.get(StoreKey.CLIENT_INFO_KEY.formatKey(), clientId);
        return JsonUtil.string2Obj(clientJsonStr, ClientInfoVo.class);
    }

    public void clearClientData(String clientId) {
        cleanLocalData(clientId);
        cleanRemoteData(clientId);
    }

    private void cleanLocalData(String clientId) {
        // 发送取消订阅广播
        ManageTopicFilterMsg manageTopicFilterMsg = new ManageTopicFilterMsg();
        manageTopicFilterMsg.setClientId(clientId);
        manageTopicFilterMsg.setManageType(ManageTopicFilterMsg.ManageType.UN_SUBSCRIBE.getKey());
        stringRedisTemplate.convertAndSend(ChannelKey.MANAGE_TOPIC_FILTER.getKey(),
            JsonUtil.obj2String(manageTopicFilterMsg));
    }

    private void cleanRemoteData(String clientId) {
        HashOperations<String, String, Integer> stringObjectObjectHashOperations = stringRedisTemplate.opsForHash();
        Map<String, Integer> clientTopicFilterMap =
            stringObjectObjectHashOperations.entries(StoreKey.CLIENT_TOPIC_FILTER_KEY.formatKey(clientId));
        String messageKey = StoreKey.MESSAGE_KEY.formatKey(clientId);
        String recMessageKey = StoreKey.REC_MESSAGE_KEY.formatKey(clientId);
        String relMessageKey = StoreKey.REL_MESSAGE_KEY.formatKey(clientId);
        String clientTopicFilterKey = StoreKey.CLIENT_TOPIC_FILTER_KEY.formatKey(clientId);
        String genMessageIdKey = StoreKey.GEN_MESSAGE_ID_KEY.formatKey(clientId);
        stringRedisTemplate.execute(new SessionCallback<Void>() {
            @SuppressWarnings({"unchecked", "NullableProblems"})
            @Override
            public Void execute(RedisOperations operations) throws DataAccessException {
                // 移除订阅关系
                for (Map.Entry<String, Integer> clientTopicFilter : clientTopicFilterMap.entrySet()) {
                    operations.opsForHash().delete((StoreKey.TOPIC_FILTER_KEY.formatKey(clientTopicFilter.getKey())),
                        clientId);
                }
                // 移除客户端的相关数据
                operations.delete(
                    Sets.newHashSet(clientTopicFilterKey, messageKey, recMessageKey, relMessageKey, genMessageIdKey));
                // 移除会话信息
                operations.opsForHash().delete(StoreKey.CLIENT_INFO_KEY.formatKey(), clientId);
                return null;
            }
        });
    }

    public void saveClientInfo(ClientInfoVo clientInfoVo) {
        stringRedisTemplate.opsForHash().put(StoreKey.CLIENT_INFO_KEY.formatKey(), clientInfoVo.getClientId(),
            JsonUtil.obj2String(clientInfoVo));
    }

    public List<Integer> subscribe(String clientId, List<TopicVo> topicVoList) {
        List<Integer> subscribeResultList = Lists.newArrayList();
        ManageTopicFilterMsg manageTopicFilterMsg = new ManageTopicFilterMsg();
        manageTopicFilterMsg.setClientId(clientId);
        manageTopicFilterMsg.setTopicVoList(topicVoList);
        manageTopicFilterMsg.setManageType(ManageTopicFilterMsg.ManageType.SUBSCRIBE.getKey());
        stringRedisTemplate.execute((RedisCallback<Void>)connection -> {
            for (TopicVo topicVo : topicVoList) {
                String clientTopicFilterKey = StoreKey.CLIENT_TOPIC_FILTER_KEY.formatKey(clientId);
                connection.hSet(clientTopicFilterKey.getBytes(), topicVo.getTopicFilter().getBytes(),
                    String.valueOf(topicVo.getQos()).getBytes());
                connection.hSet((StoreKey.TOPIC_FILTER_KEY.formatKey(topicVo.getTopicFilter())).getBytes(),
                    clientId.getBytes(), String.valueOf(topicVo.getQos()).getBytes());
            }
            return null;
        });
        stringRedisTemplate.convertAndSend(ChannelKey.MANAGE_TOPIC_FILTER.getKey(),
            JsonUtil.obj2String(manageTopicFilterMsg));
        return subscribeResultList;
    }

    public void unSubscribe(String clientId, List<TopicVo> topicVoList) {
        for (TopicVo topicVo : topicVoList) {
            TopicUtil.validateTopicFilter(topicVo.getTopicFilter());
        }
        // 发送取消订阅广播
        ManageTopicFilterMsg manageTopicFilterMsg = new ManageTopicFilterMsg();
        manageTopicFilterMsg.setClientId(clientId);
        manageTopicFilterMsg.setManageType(ManageTopicFilterMsg.ManageType.UN_SUBSCRIBE.getKey());
        manageTopicFilterMsg.setTopicVoList(topicVoList);
        stringRedisTemplate.convertAndSend(ChannelKey.MANAGE_TOPIC_FILTER.getKey(),
            JsonUtil.obj2String(manageTopicFilterMsg));
        stringRedisTemplate.execute((RedisCallback<Void>)connection -> {
            for (TopicVo topicVo : topicVoList) {
                connection.hDel((StoreKey.CLIENT_TOPIC_FILTER_KEY.formatKey(clientId)).getBytes(),
                    topicVo.getTopicFilter().getBytes());
                connection.hDel((StoreKey.TOPIC_FILTER_KEY.formatKey(topicVo.getTopicFilter())).getBytes(),
                    clientId.getBytes());
            }
            return null;
        });

    }

    public void dealMessage(MessageVo messageVo) {
        Integer isRetain = messageVo.getIsRetained();
        MqttQoS fromMqttQoS = MqttQoS.valueOf(messageVo.getFromQos());
        String payload = messageVo.getPayload();
        if (YesOrNo.YES.getValue().equals(isRetain)) {
            // qos == 0 || payload 为零字节，清除该主题下的保留消息
            if (MqttQoS.AT_MOST_ONCE == fromMqttQoS || StringUtils.isBlank(payload)) {
                delTopicRetainMessage(messageVo.getTopic());
            }
            // 存储保留消息
            else {
                saveTopicRetainMessage(messageVo);
            }
        }
        if (MqttQoS.EXACTLY_ONCE.equals(fromMqttQoS)) {
            saveRecMessage(messageVo);
            return;
        }
        sendMessage(messageVo);
    }

    public void sendMessage(MessageVo messageVo) {
        long startTime = System.currentTimeMillis();
        // 先根据topic做匹配
        Map<String, Integer> matchMap = subscribeStore.searchSubscribe(messageVo.getTopic());
        List<MessageVo> batchSendMessageVoList = new ArrayList<>();
        ArrayList<Map.Entry<String, Integer>> matchClientList = Lists.newArrayList(matchMap.entrySet());
        for (int i = 0; i < matchClientList.size(); i++) {
            Map.Entry<String, Integer> entry = matchClientList.get(i);
            Integer toQos = Math.min(messageVo.getFromQos(), entry.getValue());
            messageVo.setToQos(toQos);
            messageVo.setToClientId(entry.getKey());
            Integer messageId = genMessageId(messageVo.getToClientId());
            if (messageId != null) {
                messageVo.setToMessageId(String.valueOf(messageId));
                switch (MqttQoS.valueOf(messageVo.getToQos())) {
                    case AT_MOST_ONCE:
                        batchSendMessageVoList.add(messageVo);
                        break;
                    case AT_LEAST_ONCE:
                    case EXACTLY_ONCE:
                        String messageKey = StoreKey.MESSAGE_KEY.formatKey(messageVo.getToClientId());
                        RedisScript<Long> redisScript = new DefaultRedisScript<>(LuaScript.SAVE_MESSAGE, Long.class);
                        Long flag = stringRedisTemplate.execute(redisScript, Lists.newArrayList(messageKey),
                            messageVo.getToMessageId(), JsonUtil.obj2String(messageVo));
                        if (flag != null) {
                            batchSendMessageVoList.add(messageVo);
                        }
                        break;
                    default:
                        break;
                }
            }
            if (batchSendMessageVoList.size() >= 100 || i == matchMap.entrySet().size() - 1) {
                stringRedisTemplate.convertAndSend(ChannelKey.SEND_MESSAGE.getKey(),
                    JsonUtil.obj2String(batchSendMessageVoList));
                batchSendMessageVoList.clear();
            }
        }
        log.info("complete send message, cost {}ms", System.currentTimeMillis() - startTime);
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

    public void saveTopicRetainMessage(MessageVo messageVo) {
        messageVo.setToQos(messageVo.getFromQos());
        // 远程存储保留消息
        stringRedisTemplate.opsForHash().put(StoreKey.RETAIN_MESSAGE_KEY.formatKey(), messageVo.getTopic(),
            JsonUtil.obj2String(messageVo));
        // 发送redis消息，存储本地保留消息
        ManageRetainMessageMsg manageRetainMessageMsg = new ManageRetainMessageMsg();
        manageRetainMessageMsg.setManageType(ManageRetainMessageMsg.ManageType.ADD.getKey());
        manageRetainMessageMsg.setMessageVo(messageVo);
        stringRedisTemplate.convertAndSend(ChannelKey.MANAGE_RETAIN_MESSAGE.getKey(),
            JsonUtil.obj2String(manageRetainMessageMsg));
    }

    public void delTopicRetainMessage(String topic) {
        // 远程删除保留消息
        stringRedisTemplate.opsForHash().delete(StoreKey.RETAIN_MESSAGE_KEY.formatKey(), topic);
        // 发送redis消息，删除本地保留消息
        ManageRetainMessageMsg manageRetainMessageMsg = new ManageRetainMessageMsg();
        manageRetainMessageMsg.setManageType(ManageRetainMessageMsg.ManageType.REMOVE.getKey());
        MessageVo messageVo = new MessageVo();
        messageVo.setTopic(topic);
        manageRetainMessageMsg.setMessageVo(messageVo);
        stringRedisTemplate.convertAndSend(ChannelKey.MANAGE_RETAIN_MESSAGE.getKey(),
            JsonUtil.obj2String(manageRetainMessageMsg));
    }

    public void sendTopicRetainMessage(String clientId, List<TopicVo> successSubscribeTopicList) {
        // 使用本地的保留消息进行匹配
        ChannelHandlerContext channelHandlerContext = SessionManager.get(clientId).getChannelHandlerContext();
        for (TopicVo topicVo : successSubscribeTopicList) {
            List<MessageVo> messageVoList = retainMessageStore.getRetainMessage(topicVo.getTopicFilter());
            for (MessageVo messageVo : messageVoList) {
                messageVo.setToClientId(clientId);
                Integer messageId = genMessageId(clientId);
                if (messageId == null) {
                    continue;
                }
                messageVo.setToMessageId(String.valueOf(messageId));
                switch (MqttQoS.valueOf(messageVo.getToQos())) {
                    case AT_LEAST_ONCE:
                    case EXACTLY_ONCE:
                        stringRedisTemplate.opsForHash().put(StoreKey.MESSAGE_KEY.formatKey(messageVo.getToClientId()),
                            messageVo.getToMessageId(), JsonUtil.obj2String(messageVo));
                        break;
                    default:
                        break;
                }
                MqttUtil.sendPublish(channelHandlerContext, messageVo);
            }
        }

    }

    public void saveRecMessage(MessageVo messageVo) {
        String recMessageKey = StoreKey.REC_MESSAGE_KEY.formatKey(messageVo.getFromClientId());
        RedisScript<Long> redisScript = new DefaultRedisScript<>(LuaScript.SAVE_REC_MESSAGE);
        stringRedisTemplate.execute(redisScript, Lists.newArrayList(recMessageKey),
            String.valueOf(messageVo.getFromMessageId()), JsonUtil.obj2String(messageVo));
    }

    public void delRecMessage(String clientId, Integer messageId) {
        stringRedisTemplate.opsForHash().delete(StoreKey.REC_MESSAGE_KEY.formatKey(clientId),
            String.valueOf(messageId));
    }

    public MessageVo getRecMessage(String clientId, Integer messageId) {
        String hashKey = StoreKey.REC_MESSAGE_KEY.formatKey(clientId);
        String messageVoStr = (String)stringRedisTemplate.opsForHash().get(hashKey, String.valueOf(messageId));
        if (StringUtils.isBlank(messageVoStr)) {
            return null;
        }
        return JsonUtil.string2Obj(messageVoStr, MessageVo.class);
    }

    public void saveRelMessage(String clientId, Integer messageId) {
        String relMessageKey = StoreKey.REL_MESSAGE_KEY.formatKey(clientId);
        RedisScript<Long> redisScript = new DefaultRedisScript<>(LuaScript.SAVE_REL_MESSAGE);
        stringRedisTemplate.execute(redisScript, Lists.newArrayList(relMessageKey), String.valueOf(messageId));
    }

    public void delRelMessage(String clientId, Integer messageId) {
        stringRedisTemplate.opsForSet().remove(StoreKey.REL_MESSAGE_KEY.formatKey(clientId), String.valueOf(messageId));
    }

    /**
     * 客户端的重连动作
     * 
     * @param clientInfoVo
     *            客户端信息
     * @param channelHandlerContext
     *            netty上下文
     * 
     */
    public void reConnect(ClientInfoVo clientInfoVo, ChannelHandlerContext channelHandlerContext) {
        // 更新客户端信息
        ClientInfoVo updateClientInfoVo = new ClientInfoVo();
        BeanUtils.copyProperties(clientInfoVo, updateClientInfoVo);
        updateClientInfoVo.setConnectTime(System.currentTimeMillis());
        saveClientInfo(updateClientInfoVo);
        // 重发未接收消息
        WorkerThreadPool.execute((a) -> {
            String messageKey = StoreKey.MESSAGE_KEY.formatKey(clientInfoVo.getClientId());
            RedisTemplateUtil.hScan(stringRedisTemplate, messageKey, "*", 10000, entry -> {
                String messageJsonStr = entry.getValue();
                // 目前只有预设的数据为空字符串
                if (StringUtils.isBlank(messageJsonStr)) {
                    return;
                }
                MessageVo messageVo = JsonUtil.string2Obj(messageJsonStr, MessageVo.class);
                Objects.requireNonNull(messageVo);
                messageVo.setIsDup(true);
                MqttUtil.sendPublish(channelHandlerContext, messageVo);
            });
        });
        // 重发PubRec报文
        WorkerThreadPool.execute((a) -> {
            String recMessageKey = StoreKey.REC_MESSAGE_KEY.formatKey(clientInfoVo.getClientId());
            RedisTemplateUtil.hScan(stringRedisTemplate, recMessageKey, "*", 10000, entry -> {
                String messageIdStr = entry.getKey();
                // 目前只有预设的数据为空字符串
                if (StringUtils.isBlank(messageIdStr)) {
                    return;
                }
                MqttUtil.sendPubRec(channelHandlerContext, Integer.valueOf(messageIdStr));
            });
        });
        // 重发PubRel报文
        WorkerThreadPool.execute((a) -> {
            String relMessageKey = StoreKey.REL_MESSAGE_KEY.formatKey(clientInfoVo.getClientId());
            RedisTemplateUtil.sScan(stringRedisTemplate, relMessageKey, "*", 10000, messageIdStr -> {
                // 目前只有预设的数据为空字符串
                if (StringUtils.isBlank(messageIdStr)) {
                    return;
                }
                MqttUtil.sendPubRel(channelHandlerContext, Integer.valueOf(messageIdStr));
            });
        });
    }

    public void refreshData(Session session) {
        stringRedisTemplate.execute(new SessionCallback<Void>() {
            @SuppressWarnings({"unchecked", "NullableProblems"})
            @Override
            public Void execute(RedisOperations operations) throws DataAccessException {
                Duration expireTime = Duration.ofMillis(session.getDataExpireTimeMilliSecond());
                String clientTopicFilterKey = StoreKey.CLIENT_TOPIC_FILTER_KEY.formatKey(session.getClientId());
                operations.opsForHash().put(clientTopicFilterKey, "", "");
                String messageKey = StoreKey.MESSAGE_KEY.formatKey(session.getClientId());
                operations.opsForHash().put(messageKey, "", "");
                String recMessageKey = StoreKey.REC_MESSAGE_KEY.formatKey(session.getClientId());
                operations.opsForHash().put(recMessageKey, "", "");
                String relMessageKey = StoreKey.REL_MESSAGE_KEY.formatKey(session.getClientId());
                operations.opsForSet().add(relMessageKey, "");
                String genMessageIdKey = StoreKey.GEN_MESSAGE_ID_KEY.formatKey(session.getClientId());
                operations.opsForValue().setIfAbsent(genMessageIdKey, "0");
                if (session.getIsCleanSession()) {
                    operations.expire(clientTopicFilterKey, expireTime);
                    operations.expire(messageKey, expireTime);
                    operations.expire(recMessageKey, expireTime);
                    operations.expire(relMessageKey, expireTime);
                    operations.expire(genMessageIdKey, expireTime);
                }
                return null;
            }
        });
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
