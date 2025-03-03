package com.ep.mqtt.server.config;

import com.ep.mqtt.server.metadata.Constant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.util.Objects;
import java.util.Properties;

/**
 * @author zbz
 * @date 2025/3/3 13:42
 */
public class EasyMqttEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (StringUtils.isBlank(Constant.CONFIG_FILE_PATH)){
            return;
        }

        FileSystemResource resource = new FileSystemResource(Constant.CONFIG_FILE_PATH);
        if (!resource.exists()) {
            return;
        }

        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(resource);
        Properties properties = yamlFactory.getObject();
        if (properties == null) {
           return;
        }
        PropertySource<?> yamlPropertySource = new PropertiesPropertySource(Objects.requireNonNull(resource.getFilename()), properties);
        environment.getPropertySources().addLast(yamlPropertySource);
    }
}
