package com.ep.mqtt.server.aliyun.config;

import com.ep.mqtt.server.aliyun.core.AliyunDeal;
import com.ep.mqtt.server.aliyun.core.RocketMqProducer;
import com.ep.mqtt.server.aliyun.core.TokenManage;
import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.util.ValidationUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author zbz
 * @date 2023/11/6 14:29
 */
@Slf4j
@ConditionalOnProperty(prefix = "mqtt.server", value = "mode", havingValue = "aliyun")
@Import({DataInputConfig.class, AliyunDeal.class, TokenManage.class})
@Configuration
public class AliyunModeConfig {

    @Autowired
    private MqttServerProperties mqttServerProperties;

    @Autowired
    private ValidationUtil validationUtil;

    @PostConstruct
    public void init(){
        checkProperties(mqttServerProperties);
    }

    private void checkProperties(MqttServerProperties mqttServerProperties){
        List<String> resultList = Lists.newArrayList();
        if (mqttServerProperties.getAliyun() == null){
            resultList.add("aliyun");
        }
        else {
            resultList = validationUtil.validate(mqttServerProperties.getAliyun());
        }
        for (String result : resultList){
            log.error("not define '{}'", result);
        }
        if (!CollectionUtils.isEmpty(resultList)){
            throw new IllegalArgumentException("error aliyun properties");
        }
    }

    @Bean(destroyMethod = "destroy")
    public RocketMqProducer rocketMqProducer() {
        return new RocketMqProducer(mqttServerProperties.getAliyun().getRocketMq());
    }

}
