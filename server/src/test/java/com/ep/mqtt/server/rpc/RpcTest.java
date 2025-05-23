package com.ep.mqtt.server.rpc;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * @author zbz
 * @date 2025/3/25 15:21
 */
public class RpcTest {

    public static void main(String[] args) {
        run();
    }

    public static void run(){
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
                .addMember("127.0.0.1");

        // 3. 创建Hazelcast集群管理器并关联到Vert.x
        HazelcastClusterManager clusterManager = new HazelcastClusterManager(hazelcastConfig);

        VertxOptions options = new VertxOptions().setClusterManager(clusterManager);

        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                Vertx vertx = res.result();

                vertx.deployVerticle(new TestVerticle());
            } else {
                // failed!
            }
        });
    }

}
