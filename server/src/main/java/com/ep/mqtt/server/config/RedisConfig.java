package com.ep.mqtt.server.config;

import com.ep.mqtt.server.listener.CleanExistSessionListener;
import com.ep.mqtt.server.listener.SendMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author zbz
 * @date 2023/8/2 17:27
 */
@Configuration
public class RedisConfig {

    @Autowired
    private MqttServerProperties mqttServerProperties;

    @Bean
    public Executor redisMqAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("redisMqThread-");
        executor.setCorePoolSize(mqttServerProperties.getListenerPoolSize());
        executor.setMaxPoolSize(mqttServerProperties.getListenerPoolSize());
        executor.initialize();
        return executor;
    }

    @Bean
    public MyMessageListenerAdapter sendMessageListenerAdapter(SendMessageListener sendMessageListener) {
        return new MyMessageListenerAdapter(sendMessageListener, "onMessage",
            sendMessageListener.getChannelKey().getKey());
    }

    @Bean
    public MyMessageListenerAdapter
        cleanExistSessionListenerAdapter(CleanExistSessionListener cleanExistSessionListener) {
        return new MyMessageListenerAdapter(cleanExistSessionListener, "onMessage",
            cleanExistSessionListener.getChannelKey().getKey());
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
        Executor redisMqAsyncExecutor, List<MyMessageListenerAdapter> myMessageListenerAdapterList) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(redisMqAsyncExecutor);
        for (MyMessageListenerAdapter myMessageListenerAdapter : myMessageListenerAdapterList) {
            container.addMessageListener(myMessageListenerAdapter,
                new PatternTopic(myMessageListenerAdapter.getChannelKey()));
        }
        return container;
    }

}
