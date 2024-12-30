package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private Integer receiveQos;

    private String receiveMessageId;

    private String fromClientId;

    private Integer sendQos;

    private String topic;

    private String sendMessageId;

    private String toClientId;

    private String payload;

    private String isReceivePuback;

    private String isReceivePubrec;

    private String isReceivePubcomp;

    private Integer validTime;

}