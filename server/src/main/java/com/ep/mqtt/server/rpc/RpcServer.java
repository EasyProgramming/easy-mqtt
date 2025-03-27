package com.ep.mqtt.server.rpc;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author zbz
 * @date 2025/3/25 15:15
 */
@Slf4j
@Component
public class RpcServer {

    private static RpcVerticle rpcVerticle;

    private Vertx vertx;

    @Resource
    private MqttServerProperties mqttServerProperties;

    public void start(Runnable afterStart){
        long start = System.currentTimeMillis();
        log.info("start rpc server");

        // 1. 创建Hazelcast配置对象
        Config hazelcastConfig = new Config();

        // 2. 配置网络参数（如组播或单播）
        NetworkConfig networkConfig = hazelcastConfig.getNetworkConfig();
        networkConfig.setPort(mqttServerProperties.getRpcPort());

        // 启用单播模式（替代默认组播）
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true)
                .addMember(mqttServerProperties.getNodeIp());

        // 3. 创建Hazelcast集群管理器并关联到Vert.x
        HazelcastClusterManager clusterManager = new HazelcastClusterManager(hazelcastConfig);

        VertxOptions options = new VertxOptions().setClusterManager(clusterManager);

        Vertx.clusteredVertx(options, clusterResult -> {
            if (clusterResult.succeeded()) {
                vertx = clusterResult.result();
                rpcVerticle = new RpcVerticle();

                vertx.deployVerticle(rpcVerticle).onComplete((vertxResult)->{
                    if (vertxResult.succeeded()){
                        log.info("complete start rpc server, cost [{}ms]", System.currentTimeMillis() - start);

                        afterStart.run();
                    }
                    else {
                        log.error("runner run error", vertxResult.cause());
                    }
                });
            } else {
                log.error("runner run error", clusterResult.cause());
            }
        });
    }

    public void stop(){
        vertx.close();
    }

    public static RpcVerticle getRpcVerticle() {
        return rpcVerticle;
    }
}
