package com.ep.mqtt.server.vo;

import lombok.Data;

/**
 * @author zbz
 * @date 2023/7/16 10:57
 */
@Data
public class ClientInfoVo {

    /**
     * 客户端id
     */
    private String clientId;

    /**
     * 连接时间
     */
    private Long connectTime;

}
