package com.ep.mqtt.server.aliyun.config;

import com.ep.mqtt.server.aliyun.core.DataInputProcessor;
import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.deal.Deal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @author zbz
 * @date 2023/11/6 16:25
 */
@ConditionalOnBean(name = "rocketMqProducer")
@ConditionalOnProperty(prefix = "mqtt.server.aliyun.data-transfer", value = "input-rule-list")
public class DataInputConfig {

    private DataInputProcessor dataInputProcessor;

    @Autowired
    private MqttServerProperties mqttServerProperties;

    @Resource
    private Deal deal;

    @PostConstruct
    public void init() {
        // 根据配置启动数据流入的consumer
        dataInputProcessor = new DataInputProcessor(mqttServerProperties.getAliyun().getDataTransfer().getInputRuleList(),
                mqttServerProperties.getAliyun().getRocketMq(), deal);
        dataInputProcessor.start();
    }


    @PreDestroy
    public void destroy(){
        // 关闭consumer
        dataInputProcessor.shutdown();
    }

}
