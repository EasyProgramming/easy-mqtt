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
@TableName(value = "client_subscribe")
public class ClientSubscribeDto {
    @TableId
    private Long id;

    private String clientId;

    private String topicFilter;

    private Integer subscribeTime;

    private Qos qos;

}