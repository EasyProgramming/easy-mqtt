package com.ep.mqtt.server.deal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author zbz
 * @date 2023/11/9 15:08
 */
@Slf4j
@ConditionalOnProperty(prefix = "mqtt.server", value = "mode", havingValue = "default")
@Component
public class EasyMqttDeal extends DefaultDeal {
}
