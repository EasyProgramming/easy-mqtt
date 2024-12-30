package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Data
@TableName(value = "client")
public class ClientDto {
    @TableId
    private Long id;

    private String clientId;

    private Integer lastConnectTime;

    private Integer createTime;

}