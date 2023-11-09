package com.ep.mqtt.server.config;

import com.ep.mqtt.server.aliyun.RocketMqProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author zbz
 * @date 2023/11/6 14:29
 */
@ConditionalOnProperty(prefix = "mqtt.server", value = "mode", havingValue = "aliyun")
@Import({DataInputConfig.class})
@Configuration
public class AliyunConfig {

    @Autowired
    private MqttServerProperties mqttServerProperties;

    @ConditionalOnProperty(prefix = "mqtt.server.aliyun.data-transfer.rocket-mq", value = "name-server")
    @Bean(destroyMethod = "destroy")
    public RocketMqProducer rocketMqProducer() {
        return new RocketMqProducer(mqttServerProperties.getAliyun().getDataTransfer().getRocketMq());
    }

}
