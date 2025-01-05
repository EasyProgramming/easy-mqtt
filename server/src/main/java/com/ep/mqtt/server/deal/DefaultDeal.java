package com.ep.mqtt.server.deal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.db.dao.ClientDao;
import com.ep.mqtt.server.db.dao.ClientSubscribeDao;
import com.ep.mqtt.server.db.dao.ReceiveMessageDao;
import com.ep.mqtt.server.db.dao.SendMessageDao;
import com.ep.mqtt.server.db.dto.ClientDto;
import com.ep.mqtt.server.metadata.*;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.ep.mqtt.server.raft.transfer.TransferData;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.store.TopicFilterStore;
import com.ep.mqtt.server.store.TopicStore;
import com.ep.mqtt.server.util.*;
import com.ep.mqtt.server.vo.MessageVo;
import com.ep.mqtt.server.vo.TopicVo;
import com.google.common.collect.Lists;

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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TopicFilterStore topicFilterStore;

    @Resource
    private MqttServerProperties mqttServerProperties;

    @Resource
    private TopicStore topicStore;

    @Resource
    private ClientDao clientDao;

    @Resource
    private ClientSubscribeDao clientSubscribeDao;

    @Resource
    private ReceiveMessageDao receiveMessageDao;

    @Resource
    private SendMessageDao sendMessageDao;

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
        // TODO: 2025/1/5 在关于消息的异步任务中，如果发现消息不存在，则视为执行成功
        receiveMessageDao.deleteByFromClientId(clientId);
        sendMessageDao.deleteByToClientId(clientId);
    }

    public List<Integer> subscribe(String clientId, List<TopicVo> topicVoList) {
        List<Integer> subscribeResultList = Lists.newArrayList();
        for (TopicVo topicVo : topicVoList) {
            try {
                TopicUtil.validateTopicFilter(topicVo.getTopicFilter());

                stringRedisTemplate.opsForHash().put(StoreKey.CLIENT_TOPIC_FILTER_KEY.formatKey(clientId),
                    topicVo.getTopicFilter(), String.valueOf(topicVo.getQos()));

                stringRedisTemplate.opsForHash().put(StoreKey.TOPIC_FILTER_KEY.formatKey(topicVo.getTopicFilter()),
                    clientId, String.valueOf(topicVo.getQos()));

                EasyMqttRaftClient.syncSend(
                    JsonUtil.obj2String(new TransferData(RaftCommand.ADD_TOPIC_FILTER, topicVo.getTopicFilter())));

                subscribeResultList.add(topicVo.getQos());
            } catch (Exception e) {
                subscribeResultList.add(MqttQoS.FAILURE.value());
            }
        }
        return subscribeResultList;
    }

    public void unSubscribe(String clientId, List<TopicVo> topicVoList) {
        for (TopicVo topicVo : topicVoList) {
            TopicUtil.validateTopicFilter(topicVo.getTopicFilter());
        }

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
        if (YesOrNo.YES.getNumber().equals(isRetain)) {
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
        Map<String, Integer> matchMap = topicFilterStore.searchSubscribe(messageVo.getTopic());
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
        EasyMqttRaftClient
            .syncSend(JsonUtil.obj2String(new TransferData(RaftCommand.ADD_TOPIC.name(), messageVo.getTopic())));

        // 远程存储保留消息
        messageVo.setToQos(messageVo.getFromQos());
        stringRedisTemplate.opsForValue().set(StoreKey.RETAIN_MESSAGE_KEY.formatKey(messageVo.getTopic()),
            JsonUtil.obj2String(messageVo));
    }

    public void delTopicRetainMessage(String topic) {
        // 远程删除保留消息
        stringRedisTemplate.delete(StoreKey.RETAIN_MESSAGE_KEY.formatKey(topic));
    }

    public void sendTopicRetainMessage(String clientId, List<TopicVo> successSubscribeTopicList) {
        ChannelHandlerContext channelHandlerContext = SessionManager.get(clientId).getChannelHandlerContext();
        for (TopicVo topicVo : successSubscribeTopicList) {
            List<MessageVo> messageVoList = topicStore.getRetainMessage(topicVo.getTopicFilter());
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
            // 查询qos=1、未puback发送消息记录，重试推送publish报文任务

            // 查询qos=2、未comp发送该消息记录，其中未rec的重试推送publish报文任务，已rec需要重试推送pubrel报文任务

            // 这里重试任务，即根据任务id更新任务状态为READY的任务执行时间

        });
    }

    private void saveClientInfo(String clientId, boolean isCleanSession) {
        ClientDto clientDto = new ClientDto();
        clientDto.setClientId(clientId);
        clientDto.setIsCleanSession(isCleanSession ? YesOrNo.YES : YesOrNo.NO);

        Long now = System.currentTimeMillis();
        clientDto.setCreateTime(now);
        clientDto.setLastConnectTime(now);

        clientDao.insert(clientDto);
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
