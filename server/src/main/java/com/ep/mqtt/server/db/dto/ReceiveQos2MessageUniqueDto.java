package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * @author : zbz
 * @date : 2025/2/2
 */
@Data
@TableName(value = "receive_qos2_message_unique")
public class ReceiveQos2MessageUniqueDto {
    @TableId
    private Long id;

    private String receivePacketId;

    private String fromClientId;

    private Long receiveMessageId;
}
