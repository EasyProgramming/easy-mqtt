package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private Integer receiveQos;

    private String topic;

}