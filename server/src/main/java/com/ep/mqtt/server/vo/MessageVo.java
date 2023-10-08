package com.ep.mqtt.server.vo;

import lombok.Data;

/**
 * @author zbz
 * @date 2023/7/29 11:28
 */
@Data
public class MessageVo {

    /**
     * 收到的qos等级
     */
    private Integer fromQos;

    /**
     * 发送的qos等级
     */
    private Integer toQos;

    /**
     * 主题
     */
    private String topic;

    /**
     * 发送的消息id
     */
    private String toMessageId;

    /**
     * 收到的消息id
     */
    private Integer fromMessageId;

    /**
     * 保留消息标志
     */
    private Integer isRetained;

    /**
     * 消息来源客户端id
     */
    private String fromClientId;

    /**
     * 消息目的地客户端id
     */
    private String toClientId;

    /**
     * 消息内容
     */
    private String payload;

    /**
     * 是否重复
     */
    private Boolean isDup;

}
