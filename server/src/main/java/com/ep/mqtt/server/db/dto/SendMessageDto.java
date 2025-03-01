package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;

import lombok.Data;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Data
@TableName(value = "send_message")
public class SendMessageDto {
    @TableId
    private Long id;

    private Qos receiveQos;

    private Integer receivePacketId;

    private String fromClientId;

    private Qos sendQos;

    private String topic;

    private Integer sendPacketId;

    private String toClientId;

    private String payload;

    private YesOrNo isReceivePubRec;

    private Long validTime;

    private YesOrNo isRetain;
}