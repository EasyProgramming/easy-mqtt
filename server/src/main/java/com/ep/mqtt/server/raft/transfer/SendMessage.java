package com.ep.mqtt.server.raft.transfer;

import com.ep.mqtt.server.metadata.Qos;

import lombok.Data;

/**
 * @author zbz
 * @date 2025/2/27 15:16
 */
@Data
public class SendMessage {

    private Qos sendQos;

    private String topic;

    private Integer sendPacketId;

    private String toClientId;

    private String payload;

    private Boolean isDup;

    private Boolean isRetain;
}
