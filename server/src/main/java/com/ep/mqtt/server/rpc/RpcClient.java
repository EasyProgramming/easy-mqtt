package com.ep.mqtt.server.rpc;

import org.springframework.stereotype.Component;

/**
 * @author zbz
 * @date 2025/3/25 15:29
 */
@Component
public class RpcClient {

    public void broadcast(){
        RpcServer.getRpcVerticle().getEventBus().publish("command", "data");
    }

}
