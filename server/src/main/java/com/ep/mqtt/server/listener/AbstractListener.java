package com.ep.mqtt.server.listener;

import com.ep.mqtt.server.metadata.ChannelKey;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/9/2 10:34
 */
@Slf4j
public abstract class AbstractListener<T> {

    public void onMessage(String message) {
        log.debug("receive message : {}", message);
        deal(convertMessage(message));
    }

    public abstract void deal(T message);

    /**
     * 获取标识
     * 
     * @return 渠道标识
     */
    public abstract ChannelKey getChannelKey();

    /**
     * 获取消息类型
     * 
     * @return 消息类型
     */
    public abstract T convertMessage(String messageStr);
}
