package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author zbz
 * @date 2025/2/27 14:19
 */
@Data
@TableName(value = "message_id_progress")
public class MessageIdProgressDto {
    @TableId
    private Long id;

    private String clientId;

    private Long progress;

}