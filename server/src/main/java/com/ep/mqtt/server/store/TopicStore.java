package com.ep.mqtt.server.store;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ep.mqtt.server.metadata.StoreKey;
import com.ep.mqtt.server.util.JsonUtil;
import com.ep.mqtt.server.util.TopicUtil;
import com.ep.mqtt.server.vo.MessageVo;

import io.vertx.core.impl.ConcurrentHashSet;
import lombok.extern.slf4j.Slf4j;

/**
 * 保留消息本地存储
 * 
 * @author zbz
 * @date 2023/9/2 16:17
 */
@Slf4j
@Component
public class TopicStore {

    private static final ConcurrentHashSet<String> TOPIC_SET = new ConcurrentHashSet<>();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public List<MessageVo> getRetainMessage(String topicFilter) {
        List<MessageVo> retainMessageList = new ArrayList<>();
        TOPIC_SET.forEach((topic) -> {
            if (TopicUtil.match(topicFilter, topic)) {
                // 查询遗嘱消息
                String messageVoStr =
                    stringRedisTemplate.opsForValue().get(StoreKey.RETAIN_MESSAGE_KEY.formatKey(topic));
                if (StringUtils.isBlank(messageVoStr)) {
                    return;
                }
                MessageVo messageVo = JsonUtil.string2Obj(messageVoStr, MessageVo.class);
                if (messageVo == null) {
                    return;
                }
                retainMessageList.add(messageVo);
            }
        });
        return retainMessageList;
    }

    public static void add(String topic) {
        TOPIC_SET.add(topic);
    }

    public static void remove(String topic) {
        TOPIC_SET.remove(topic);
    }

    public static ConcurrentHashSet<String> getTopicSet() {
        return TOPIC_SET;
    }

}
