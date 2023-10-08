package com.ep.mqtt.server.util;

import com.ep.mqtt.server.metadata.YesOrNo;
import com.ep.mqtt.server.vo.MessageVo;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;

/**
 * @author : zbz
 * @date : 2023/8/30
 */
public class MqttUtil {

    public static void sendPublish(ChannelHandlerContext channelHandlerContext, MessageVo messageVo) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, messageVo.getIsDup(),
            MqttQoS.valueOf(messageVo.getToQos()), YesOrNo.valueOf(messageVo.getIsRetained()), 0);
        MqttPublishVariableHeader mqttVariableHeader =
            new MqttPublishVariableHeader(messageVo.getTopic(), Integer.parseInt(messageVo.getToMessageId()));
        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(mqttFixedHeader, mqttVariableHeader,
            Unpooled.buffer().writeBytes(messageVo.getPayload().getBytes()));
        channelHandlerContext.writeAndFlush(mqttPublishMessage);
    }

    public static void sendPubRel(ChannelHandlerContext channelHandlerContext, Integer messageId) {
        MqttFixedHeader mqttFixedHeader =
            new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        channelHandlerContext
            .writeAndFlush(MqttMessageFactory.newMessage(mqttFixedHeader, mqttMessageIdVariableHeader, null));
    }

    public static void sendPubRec(ChannelHandlerContext channelHandlerContext, Integer messageId) {
        MqttFixedHeader mqttFixedHeader =
            new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        channelHandlerContext
            .writeAndFlush(MqttMessageFactory.newMessage(mqttFixedHeader, mqttMessageIdVariableHeader, null));
    }
}
