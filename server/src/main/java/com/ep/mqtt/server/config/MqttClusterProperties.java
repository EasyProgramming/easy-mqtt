package com.ep.mqtt.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author zbz
 * @date 2023/7/1 11:26
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt.cluster")
public class MqttClusterProperties {

    /**
     * 当前节点信息
     */
    private Node currentNode;

    /**
     * 其它节点
     */
    private List<Node> otherNodes;

    @Data
    public static class Node {

        private String id;

        private String address;

    }

}
