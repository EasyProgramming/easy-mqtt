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

    public static void distributedLock(String key, Runnable runnable){
        RpcServer.getVertx().sharedData().getLock(key, (result)->{
            if (!result.succeeded()){
                log.error("获取锁失败, key: [{}]", key, result.cause());

                throw new RuntimeException("加锁失败", result.cause());
            }

            try {
                runnable.run();
            }
            catch (Throwable e){
                log.error("处理失败, key: [{}]", key, e);
                throw e;
            }
            finally {
                result.result().release();
            }
        });
    }
}
