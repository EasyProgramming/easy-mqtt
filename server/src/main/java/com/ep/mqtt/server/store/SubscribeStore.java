package com.ep.mqtt.server.store;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.metadata.StoreKey;
import com.ep.mqtt.server.util.RedisTemplateUtil;
import com.ep.mqtt.server.util.TopicUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/8/17 15:31
 */
@Slf4j
@Component
public class SubscribeStore {

    private static final ConcurrentMap<String, ConcurrentMap<String, Integer>> TOPIC_FILTER_STORE =
        new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ConcurrentMap<String, Integer>> CLIENT_TOPIC_FILTER_STORE =
        new ConcurrentHashMap<>();
    private static final ScheduledThreadPoolExecutor CLEAN_EXPIRE_TOPIC_FILTER_SCHEDULED_THREAD_POOL_EXECUTOR =
        new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("cleanExpireTopicFilter-%s").build());

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void init() {
        initData();
        CLEAN_EXPIRE_TOPIC_FILTER_SCHEDULED_THREAD_POOL_EXECUTOR
            .scheduleWithFixedDelay(new CleanExpireTopicFilterRunnable(), 0, 10, TimeUnit.MINUTES);
    }

    private void initData() {
        String scanKey = Constant.TOPIC_FILTER_KEY_PREFIX + Constant.STORE_KEY_SPLIT + "*";
        Set<String> topicFilterKeySet = RedisTemplateUtil.scan(stringRedisTemplate, scanKey, 10000);
        HashOperations<String, String, String> stringObjectObjectHashOperations = stringRedisTemplate.opsForHash();
        for (String topicFilterKey : topicFilterKeySet) {
            Map<String, String> subscribeList = stringObjectObjectHashOperations.entries(topicFilterKey);
            String topicFilter =
                StringUtils.removeStart(topicFilterKey, Constant.TOPIC_FILTER_KEY_PREFIX + Constant.STORE_KEY_SPLIT);
            for (Map.Entry<String, String> subscribe : subscribeList.entrySet()) {
                subscribe(topicFilter, subscribe.getKey(), Integer.valueOf(subscribe.getValue()));
            }
        }
    }

    public void subscribe(String topicFilter, String clientId, Integer qos) {
        Map<String, Integer> clientMap =
            TOPIC_FILTER_STORE.computeIfAbsent(topicFilter, (key) -> new ConcurrentHashMap<>(16));
        // 如果不存在或者老的订阅 qos 比较小也重新设置
        Integer existingQos;
        existingQos = clientMap.get(clientId);
        if (existingQos == null || existingQos < qos) {
            clientMap.put(clientId, qos);
        }
        Map<String, Integer> topicFilterMap =
            CLIENT_TOPIC_FILTER_STORE.computeIfAbsent(clientId, (key) -> new ConcurrentHashMap<>(16));
        existingQos = topicFilterMap.get(topicFilter);
        if (existingQos == null || existingQos < qos) {
            topicFilterMap.put(topicFilter, qos);
        }
    }

    public void unSubscribe(String topicFilter, String clientId) {
        ConcurrentMap<String, Integer> map = TOPIC_FILTER_STORE.get(topicFilter);
        if (map != null) {
            map.remove(clientId);
        }
        map = CLIENT_TOPIC_FILTER_STORE.get(clientId);
        if (map != null) {
            map.remove(topicFilter);
        }
    }

    public void unSubscribe(String clientId) {
        for (Map.Entry<String, ConcurrentMap<String, Integer>> entry : TOPIC_FILTER_STORE.entrySet()) {
            entry.getValue().remove(clientId);
        }
        CLIENT_TOPIC_FILTER_STORE.remove(clientId);
    }

    public Map<String, Integer> searchSubscribe(String topicName) {
        // 排除重复订阅，例如： /test/# 和 /# 只发一份
        Map<String, Integer> subscribeMap = new HashMap<>(32);
        Set<String> topicFilterSet = TOPIC_FILTER_STORE.keySet();
        for (String topicFilter : topicFilterSet) {
            if (TopicUtil.match(topicFilter, topicName)) {
                ConcurrentMap<String, Integer> data = TOPIC_FILTER_STORE.get(topicFilter);
                if (data != null && !data.isEmpty()) {
                    data.forEach((clientId, qos) -> subscribeMap.merge(clientId, qos, Math::max));
                }
            }
        }
        return subscribeMap;
    }

    /**
     * 定时清理过期的topicFilter
     */
    public class CleanExpireTopicFilterRunnable implements Runnable {

        @Override
        public void run() {
            Date startDate = new Date();
            log.info("start clean expire topic filter, {}", DateFormatUtils.format(startDate, "yyyy-MM-dd HH:mm:ss"));
            try {
                String scanKey = Constant.TOPIC_FILTER_KEY_PREFIX + Constant.STORE_KEY_SPLIT + "*";
                // 按远程topicFilter查询client，按client查询是否存在，不存在则删除远程，删除本地
                RedisTemplateUtil.scan(stringRedisTemplate, scanKey, 10000,
                    topicFilter -> RedisTemplateUtil.hScan(stringRedisTemplate, topicFilter, "*", 10000, entry -> {
                        String clientId = new String(entry.getKey(), StandardCharsets.UTF_8);
                        Integer qos = Integer.parseInt(new String(entry.getValue(), StandardCharsets.UTF_8));
                        String key = StoreKey.CLIENT_TOPIC_FILTER_KEY.formatKey(clientId);
                        Boolean isKeyExist = stringRedisTemplate.hasKey(key);
                        if (isKeyExist == null || !isKeyExist) {
                            // 删除远程
                            stringRedisTemplate.opsForHash().delete(topicFilter, clientId);
                            // 删除本地
                            unSubscribe(topicFilter, clientId);
                        } else {
                            // 让本地数据与远程数据保持一致
                            subscribe(topicFilter, clientId, qos);
                        }
                    }));
            } catch (Throwable throwable) {
                log.error("clean expire topic filter occurred error", throwable);
            } finally {
                Date completeDate = new Date();
                log.info("complete clean expire topic filter, {}, cost {}ms",
                    DateFormatUtils.format(completeDate, "yyyy-MM-dd HH:mm:ss"),
                    completeDate.getTime() - startDate.getTime());
            }
        }

    }

}
