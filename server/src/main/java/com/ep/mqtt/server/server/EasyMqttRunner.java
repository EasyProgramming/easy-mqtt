package com.ep.mqtt.server.server;

import com.ep.mqtt.server.job.AsyncJobEngine;
import com.ep.mqtt.server.raft.server.EasyMqttRaftServeSwitch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author zbz
 * @date 2025/3/13 16:49
 */
@Slf4j
@Component
public class EasyMqttRunner implements ApplicationRunner, DisposableBean {

    @Resource
    private EasyMqttRaftServeSwitch easyMqttRaftServeSwitch;

    @Resource
    private AsyncJobEngine asyncJobEngine;

    @Resource
    private MqttServer mqttServer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        easyMqttRaftServeSwitch.start();

        mqttServer.start();

        asyncJobEngine.start();
    }

    @Override
    public void destroy() throws Exception {
        asyncJobEngine.stop();

        mqttServer.stop();

        easyMqttRaftServeSwitch.stop();
    }
}
