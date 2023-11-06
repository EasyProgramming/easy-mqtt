package com.ep.mqtt.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author zbz
 * @date 2023/7/1 11:26
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt.server")
public class MqttServerProperties {

    /**
     * 服务器模式
     * @see com.ep.mqtt.server.metadata.MqttServerMode
     */
    private String mode;

    /**
     * 是否开启Epoll模式, linux下建议开启
     */
    private Boolean isUseEpoll = false;

    /**
     * 是否开启ssl
     */
    private Boolean isOpenSsl = false;

    /**
     * SSL密钥文件密码
     */
    private String sslCertificatePassword;

    /**
     * SSL证书文件的绝对路径，只支持pfx格式的证书
     */
    private String sslCertificatePath;

    /**
     * tcp端口（mqtt协议的端口）
     */
    private Integer tcpPort = 8081;

    /**
     * websocket端口
     */
    private Integer websocketPort = 8082;

    /**
     * websocket连接地址
     */
    private String websocketPath = "/websocket";

    /**
     * 认证接口地址，如果为null或空字符串则不鉴权
     */
    private String authenticationUrl;

    /**
     * 监听器的线程池大小
     */
    private Integer listenerPoolSize = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 处理消息线程池的大小
     */
    private Integer dealMessageThreadPoolSize = Runtime.getRuntime().availableProcessors() * 3;

    /**
     * 阿里云相关配置
     */
    private Aliyun aliyun;

    /**
     * 阿里云相关配置
     */
    @Data
    public static class Aliyun {

        /**
         * 授权方式
         * @see com.ep.mqtt.server.metadata.AliyunAuthWay
         */
        private String authWay;

        /**
         * 数据流转
         */
        private DataTransfer dataTransfer;

        /**
         * 数据流转
         */
        @Data
        public static class DataTransfer {

            /**
             * rocketmq相关配置
             */
            private RocketMq rocketMq;

            /**
             * <p>数据流入规则</p>
             * <p>mq（fromTopic） -> mqtt（toTopic） -> 客户端</p>
             */
            private List<TopicMappingRule> inputRuleList;

            /**
             * <p>数据流出规则</p>
             * <p>客户端 -> mqtt（fromTopic） -> mq（toTopic）</p>
             */
            private List<TopicMappingRule> outputRuleList;

        }

        /**
         * rocketmq相关配置
         */
        @Data
        public static class RocketMq {

            /**
             * rocketmq的nameServer（多个地址，用英文分号进行拼接）
             */
            private String nameServer;

        }

        /**
         * topic映射规则
         */
        @Data
        public static class TopicMappingRule {

            /**
             * 来源topic
             */
            private String fromTopic;

            /**
             * 目标topic
             */
            private String toTopic;

        }

    }
}
