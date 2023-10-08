package com.ep.mqtt.server.config;

import com.ep.mqtt.server.util.WorkerThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author zbz
 * @date 2023/9/14 11:49
 */
@Configuration
public class WorkerThreadPoolConfig {

    @Autowired
    private MqttServerProperties mqttServerProperties;

    @PostConstruct
    public void init(){
        WorkerThreadPool.init(mqttServerProperties.getWorkerThreadPoolSize());
    }

}
