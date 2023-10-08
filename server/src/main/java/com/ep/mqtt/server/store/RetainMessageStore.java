package com.ep.mqtt.server.store;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ep.mqtt.server.metadata.StoreKey;
import com.ep.mqtt.server.util.JsonUtil;
import com.ep.mqtt.server.util.RedisTemplateUtil;
import com.ep.mqtt.server.util.TopicUtil;
import com.ep.mqtt.server.vo.MessageVo;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * 保留消息本地存储
 * 
 * @author zbz
 * @date 2023/9/2 16:17
 */
@Slf4j
@Component
public class RetainMessageStore {

    /**
     * 保持消息 topic: Message
     */
    private static final ConcurrentMap<String, MessageVo> RETAIN_MESSAGE_STORE = new ConcurrentHashMap<>();

    private static final ScheduledThreadPoolExecutor CHECK_DATA_SCHEDULED_THREAD_POOL_EXECUTOR =
        new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("checkRetainMessageData-%s").build());

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void init() {
        initData();
        CHECK_DATA_SCHEDULED_THREAD_POOL_EXECUTOR.scheduleWithFixedDelay(new CheckDataRunnable(), 0, 10,
            TimeUnit.MINUTES);
    }

    private void initData() {
        RedisTemplateUtil.hScan(stringRedisTemplate, StoreKey.RETAIN_MESSAGE_KEY.formatKey(), "*", 10000, entry -> {
            String topic = new String(entry.getKey(), StandardCharsets.UTF_8);
            MessageVo messageVo =
                JsonUtil.string2Obj(new String(entry.getValue(), StandardCharsets.UTF_8), MessageVo.class);
            addRetainMessage(topic, messageVo);
        });
    }

    public void addRetainMessage(String topic, MessageVo messageVo) {
        RETAIN_MESSAGE_STORE.put(topic, messageVo);
    }

    public void removeRetainMessage(String topic) {
        RETAIN_MESSAGE_STORE.remove(topic);
    }

    public List<MessageVo> getRetainMessage(String topicFilter) {
        List<MessageVo> retainMessageList = new ArrayList<>();
        RETAIN_MESSAGE_STORE.forEach((topic, message) -> {
            if (TopicUtil.match(topicFilter, topic)) {
                retainMessageList.add(message);
            }
        });
        return retainMessageList;
    }

    /**
     * 检查数据保持一致
     */
    public class CheckDataRunnable implements Runnable {

        @Override
        public void run() {
            Date startDate = new Date();
            log.info("start check retain message data, {}", DateFormatUtils.format(startDate, "yyyy-MM-dd HH:mm:ss"));
            Set<String> remoteTopicSet = Sets.newHashSet();
            RedisTemplateUtil.hScan(stringRedisTemplate, StoreKey.RETAIN_MESSAGE_KEY.formatKey(), "*", 10000, entry -> {
                String topic = new String(entry.getKey(), StandardCharsets.UTF_8);
                MessageVo messageVo =
                    JsonUtil.string2Obj(new String(entry.getValue(), StandardCharsets.UTF_8), MessageVo.class);
                if (getRetainMessage(topic) == null) {
                    addRetainMessage(topic, messageVo);
                }
                remoteTopicSet.add(topic);
            });
            Set<String> localTopicSet = RETAIN_MESSAGE_STORE.keySet();
            localTopicSet.removeAll(remoteTopicSet);
            for (String localTopic : localTopicSet) {
                removeRetainMessage(localTopic);
            }
            Date completeDate = new Date();
            log.info("complete check retain message data, {}, cost {}ms",
                DateFormatUtils.format(completeDate, "yyyy-MM-dd HH:mm:ss"),
                completeDate.getTime() - startDate.getTime());
        }

    }
}
