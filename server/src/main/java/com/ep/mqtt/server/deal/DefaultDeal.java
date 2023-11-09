package com.ep.mqtt.server.deal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 请求broker
 *
 * @author zbz
 * @date 2023/7/15 17:10
 */
@Slf4j
@ConditionalOnProperty(prefix = "mqtt.server", value = "mode", havingValue = "default")
@Component
public class DefaultDeal extends Deal {

}
