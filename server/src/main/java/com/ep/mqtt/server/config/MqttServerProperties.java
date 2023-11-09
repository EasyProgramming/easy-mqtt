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
             * <p>mq（rocketMqTopic） -> mqtt（mqttTopic） -> 客户端</p>
             */
            private List<TopicMapRule> inputRuleList;

            /**
             * <p>数据流出规则</p>
             * <p>客户端 -> mqtt（mqttTopic） -> mq（rocketMqTopic）</p>
             */
            private List<TopicMapRule> outputRuleList;

        }

        /**
         * rocketmq相关配置
         */
        @Data
        public static class RocketMq {

            /**
             * 消息队列RocketMQ版控制台创建的Group ID
             */
            private String groupId;

            /**
             * AccessKey ID，阿里云身份验证标识
             */
            private String accessKey;

            /**
             * AccessKey Secret，阿里云身份验证密钥
             */
            private String secretKey;

            /**
             * 设置TCP接入域名，进入消息队列RocketMQ版控制台实例详情页面的接入点区域查看
             */
            private String nameserverAddr;

        }

        /**
         * topic映射规则
         */
        @Data
        public static class TopicMapRule {

            /**
             * rocketmq topic
             */
            private RocketMqTopic rocketMqTopic;

            /**
             * mqtt topic
             */
            private MqttTopic mqttTopic;

        }

        /**
         * rocketmq topic
         */
        @Data
        public static class RocketMqTopic {

            /**
             * topic
             */
            private String topic;

            /**
             * 消息类型
             * @see com.ep.mqtt.server.metadata.RocketMqMessageType
             */
            private String messageType;

        }

        /**
         * mqtt topic
         */
        @Data
        public static class MqttTopic {

            /**
             * topic
             */
            private String topic;
        }

    }
}
