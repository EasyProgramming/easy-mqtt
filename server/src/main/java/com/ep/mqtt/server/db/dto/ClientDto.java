package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ep.mqtt.server.metadata.YesOrNo;
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

    private Long lastConnectTime;

    private Long createTime;

    private YesOrNo isCleanSession;

    private Long messageIdProgress;

}