package com.ep.mqtt.server.rpc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

/**
 * @author zbz
 * @date 2025/3/25 15:22
 */
public class TestVerticle extends AbstractVerticle {

    @Override
    public void start() {
        EventBus eventBus = vertx.eventBus();

        // 注册消息消费者
        eventBus.consumer("cluster.topic", message -> {
            System.out.println("收到消息-1: " + message.body());
            // 可以进行相应的业务处理
        });

        // 例如，定时发送一条消息到集群中
        vertx.setPeriodic(5000, id -> {
            eventBus.publish("cluster.topic", "Hello from " + vertx);
        });
    }

}