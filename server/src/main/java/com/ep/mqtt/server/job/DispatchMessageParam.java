package com.ep.mqtt.server.job;

import com.ep.mqtt.server.metadata.Qos;

import lombok.Data;

/**
 * @author : zbz
 * @date : 2025/2/1
 */
@Data
public class DispatchMessageParam {

    private Qos receiveQos;

    private String topic;

    private Integer receivePacketId;

    private String fromClientId;

    private String payload;

}
