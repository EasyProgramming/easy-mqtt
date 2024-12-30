package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;
import lombok.Data;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Data
@TableName(value = "receive_message")
public class ReceiveMessageDto {
    @TableId
    private Long id;

    private Qos receiveQos;

    private String topic;

    private String receiveMessageId;

    private String fromClientId;

    private String payload;

    private YesOrNo isSendPuback;

    private YesOrNo isReceivePubrel;

    private YesOrNo isSendPubcomp;

    private YesOrNo isComplete;

    private Integer validTime;

}