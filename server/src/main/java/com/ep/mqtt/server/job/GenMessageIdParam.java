package com.ep.mqtt.server.job;

import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;
import lombok.Data;

/**
 * @author : zbz
 * @date : 2025/2/1
 */
@Data
public class GenMessageIdParam {

    private Qos receiveQos;

    private Integer receivePacketId;

    private String fromClientId;

    private Qos sendQos;

    private String topic;

    private String toClientId;

    private String payload;

    private YesOrNo isReceivePubRec;

    private YesOrNo isRetain;

}
