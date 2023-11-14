package com.ep.mqtt.server.aliyun.core;

import com.aliyun.openservices.ons.api.*;
import com.ep.mqtt.server.config.MqttServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Properties;

/**
 * @author zbz
 * @date 2023/11/6 14:58
 */
@Slf4j
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

    public void send(String topic, String body, Properties properties) {
        Message msg = new Message(topic, "*", body.getBytes());
        msg.setUserProperties(properties);
        SendResult sendResult = producer.send(msg);
        log.debug("send result: {}", sendResult.toString());
    }

}
