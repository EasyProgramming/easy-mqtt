package com.ep.mqtt.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ep.mqtt.server.metadata.RunMode;

import lombok.Data;

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
    private Boolean isUseEpoll = true;

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
     * api的端口
     */
    private Integer apiPort = 8083;

    /**
     * 节点的地址（集群模式下，各节点的地址需要以英文逗号拼接，需要注意的是：第一个地址需要为本机的地址）
     */
    private String nodeAddress = "127.0.0.1:8084";

    /**
     * websocket连接地址
     */
    private String websocketPath = "/websocket";

    /**
     * 认证接口地址，如果为null或空字符串则不鉴权
     */
    private String authenticationUrl;

    /**
     * 运行模式
     * @see com.ep.mqtt.server.metadata.RunMode
     */
    private String runMode = RunMode.STANDALONE.getCode();

    /**
     * 数据库配置，只在集群模式下生效，目前只支持mysql
     */
    private Db db;

    @Data
    public static class Db {

        /**
         * 数据库地址
         */
        private String url;

        /**
         * 账号
         */
        private String username;

        /**
         * 密码
         */
        private String password;

    }

}
