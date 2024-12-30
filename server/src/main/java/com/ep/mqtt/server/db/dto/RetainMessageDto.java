package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ep.mqtt.server.metadata.Qos;
import lombok.Data;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Data
@TableName(value = "retain_message")
public class RetainMessageDto {
    @TableId
    private Long id;

    private String payload;

    private Qos receiveQos;

    private String topic;

}