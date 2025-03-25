package com.ep.mqtt.server.rpc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

/**
 * @author zbz
 * @date 2025/3/25 15:17
 */
public class RpcVerticle extends AbstractVerticle {

    private EventBus eventBus;

    @Override
    public void start() {
        eventBus = vertx.eventBus();

        // 发送消息
        eventBus.consumer("cluster.topic", message -> {

        });

        // 处理重复session
        eventBus.consumer("cluster.topic", message -> {

        });
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}