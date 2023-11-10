package com.ep.mqtt.server.aliyun.core;

import com.aliyun.openservices.ons.api.*;
import com.ep.mqtt.server.config.MqttServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Properties;

/**
 * @author zbz
 * @date 2023/11/6 14:58
 */
public class RocketMqProducer {

    private final Producer producer;

    public RocketMqProducer(MqttServerProperties.Aliyun.RocketMq rocketMq) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.AccessKey, rocketMq.getAccessKey());
        properties.put(PropertyKeyConst.SecretKey, rocketMq.getSecretKey());
        properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, "3000");
        properties.put(PropertyKeyConst.NAMESRV_ADDR, rocketMq.getNameserverAddr());
        producer = ONSFactory.createProducer(properties);
        producer.start();
    }

    public void destroy(){
        producer.shutdown();
    }

    public SendResult send(String topic, String body, Properties properties) {
        Message msg = new Message(topic, "*", body.getBytes());
        msg.setUserProperties(properties);
        return producer.send(msg);
    }

}
