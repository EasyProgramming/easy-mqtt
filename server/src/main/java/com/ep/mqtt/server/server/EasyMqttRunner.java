package com.ep.mqtt.server.server;

import com.ep.mqtt.server.job.AsyncJobEngine;
import com.ep.mqtt.server.raft.server.EasyMqttRaftServeSwitch;
import com.ep.mqtt.server.rpc.RpcServer;
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

    @Resource
    private RpcServer rpcServer;

    @Override
    public void run(ApplicationArguments args) {
        try {
            rpcServer.start();

            easyMqttRaftServeSwitch.start();

            mqttServer.start();

            asyncJobEngine.start();
        }
        catch (Throwable e){
            log.error("runner run error ", e);
        }
    }

    @Override
    public void destroy() {
        try {
            asyncJobEngine.stop();

            mqttServer.stop();

            easyMqttRaftServeSwitch.stop();

            rpcServer.stop();
        }
        catch (Throwable e){
            log.error("runner destroy error ", e);
        }
    }
}
