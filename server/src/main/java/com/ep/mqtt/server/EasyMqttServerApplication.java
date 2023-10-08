package com.ep.mqtt.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author zbz
 * @date 2023/7/1 11:27
 */
@EnableConfigurationProperties
@SpringBootApplication
public class EasyMqttServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyMqttServerApplication.class, args);
    }

}
