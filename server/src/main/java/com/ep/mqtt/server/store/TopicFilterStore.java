package com.ep.mqtt.server.store;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ep.mqtt.server.util.RedisTemplateUtil;
import com.ep.mqtt.server.util.TopicUtil;

import io.vertx.core.impl.ConcurrentHashSet;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/8/17 15:31
 */
@Slf4j
@Component
public class TopicFilterStore {

    private static final ConcurrentHashSet<String> TOPIC_FILTER_SET = new ConcurrentHashSet<>();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public Map<String, Integer> searchSubscribe(String topicName) {
        // 排除重复订阅，例如： /test/# 和 /# 只发一份
        Map<String, Integer> subscribeMap = new HashMap<>(32);
        for (String topicFilter : TOPIC_FILTER_SET) {
            if (!TopicUtil.match(topicFilter, topicName)) {
                continue;
            }
            RedisTemplateUtil.hScan(stringRedisTemplate, topicFilter, "*", 5000,
                entry -> subscribeMap.merge(entry.getKey(), Integer.valueOf(entry.getValue()), Math::max));
        }
        return subscribeMap;
    }

    public static void add(String topicFilter) {
        TOPIC_FILTER_SET.add(topicFilter);
    }

    public static void remove(String topicFilter) {
        TOPIC_FILTER_SET.remove(topicFilter);
    }

    public static ConcurrentHashSet<String> getTopicFilterSet() {
        return TOPIC_FILTER_SET;
    }

}
