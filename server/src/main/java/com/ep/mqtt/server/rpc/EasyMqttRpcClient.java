package com.ep.mqtt.server.rpc;

import com.ep.mqtt.server.metadata.RpcCommand;
import com.ep.mqtt.server.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2025/3/25 15:29
 */
@Slf4j
public class EasyMqttRpcClient {

    public static void broadcast(RpcCommand rpcCommand, Object data){
        RpcServer.getRpcVerticle().getEventBus().publish(rpcCommand.getCode(), JsonUtil.obj2String(data));
    }

}
