package com.ep.mqtt.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zbz
 * @date 2023/7/1 11:26
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt.server")
public class MqttServerProperties {

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
}
