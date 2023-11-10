package com.ep.mqtt.server.config;

import com.ep.mqtt.server.deal.DefaultDeal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author zbz
 * @date 2023/11/10 11:47
 */
@ConditionalOnProperty(prefix = "mqtt.server", value = "mode", havingValue = "default")
@Import({DefaultDeal.class})
@Configuration
public class DefaultModeConfig {
}
