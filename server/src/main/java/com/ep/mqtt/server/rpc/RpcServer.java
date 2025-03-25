package com.ep.mqtt.server.rpc;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author zbz
 * @date 2025/3/25 15:15
 */
@Slf4j
@Component
public class RpcServer {

    private static RpcVerticle rpcVerticle;

    private Vertx vertx;

    public void start(){
        // 1. 创建Hazelcast配置对象
        Config hazelcastConfig = new Config();

        // 2. 配置网络参数（如组播或单播）
        NetworkConfig networkConfig = hazelcastConfig.getNetworkConfig();
        networkConfig.setPort(5701)
                .setPortAutoIncrement(true);

        // 启用单播模式（替代默认组播）
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true)
                // TODO: 2025/3/25 先临时这么做
                .addMember("127.0.0.1");

        // 3. 创建Hazelcast集群管理器并关联到Vert.x
        HazelcastClusterManager clusterManager = new HazelcastClusterManager(hazelcastConfig);

        VertxOptions options = new VertxOptions().setClusterManager(clusterManager);

        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                vertx = res.result();

                rpcVerticle = new RpcVerticle();
                vertx.deployVerticle(rpcVerticle);
            } else {
                log.error("rpc server start error", res.cause());
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
