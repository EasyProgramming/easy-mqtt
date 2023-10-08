package com.ep.mqtt.server.config;

import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author : zbz
 * @date : 2023/8/21
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MyMessageListenerAdapter extends MessageListenerAdapter {

    private String channelKey;

    public MyMessageListenerAdapter(Object delegate, String defaultListenerMethod, String channelKey) {
        super(delegate, defaultListenerMethod);
        this.channelKey = channelKey;
    }

}
