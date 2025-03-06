package com.ep.mqtt.server.raft.transfer;

import com.ep.mqtt.server.metadata.Qos;
import lombok.Data;

/**
 * @author zbz
 * @date 2025/2/21 14:20
 */
@Data
public class AddRetainMessage {

    private String topic;

    private String payload;

    private Qos receiveQos;

    private Integer receivePacketId;

    private String fromClientId;

}
