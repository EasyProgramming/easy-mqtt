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

    private String receiveMessageId;

    private String fromClientId;

    private Qos sendQos;

    private String topic;

    private String sendMessageId;

    private String toClientId;

    private String payload;

    private YesOrNo isReceivePuback;

    private YesOrNo isReceivePubrec;

    private YesOrNo isReceivePubcomp;

    private Long validTime;

}