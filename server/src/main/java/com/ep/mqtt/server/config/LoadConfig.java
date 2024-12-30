package com.ep.mqtt.server.config;

import com.ep.mqtt.server.metadata.Constant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileUrlResource;

import java.net.MalformedURLException;

/**
 * @author zbz
 * @date 2023/10/7 15:38
 */
@Configuration
public class LoadConfig {

    @Bean
    public PropertySourcesPlaceholderConfigurer mqttConfig() throws MalformedURLException {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        if (StringUtils.isBlank(Constant.CONFIG_FILE_PATH)){
            throw new IllegalArgumentException("need set config file path");
        }
        yaml.setResources(new FileUrlResource(Constant.CONFIG_FILE_PATH));
        if (yaml.getObject() == null){
            throw new IllegalArgumentException("parse config file fail");
        }
        configurer.setProperties(yaml.getObject());
        return configurer;
    }

}
