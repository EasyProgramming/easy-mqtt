package com.ep.mqtt.server.util;

import com.ep.mqtt.server.metadata.Qos;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;

/**
 * @author : zbz
 * @date : 2023/8/30
 */
public class MqttUtil {

    public static void sendPublish(ChannelHandlerContext channelHandlerContext, Boolean isDup, Qos qos, Boolean isRetain, String topic,
                                   String messageId, String payload) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, isDup,
            MqttQoS.valueOf(qos.getCode()), isRetain, 0);
        MqttPublishVariableHeader mqttVariableHeader =
            new MqttPublishVariableHeader(topic, Integer.parseInt(messageId));
        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(mqttFixedHeader, mqttVariableHeader,
            Unpooled.buffer().writeBytes(payload.getBytes()));
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

    public static void sendPubAck(ChannelHandlerContext channelHandlerContext, Integer messageId) {
        channelHandlerContext.writeAndFlush(MqttMessageBuilders.pubAck().packetId(messageId).build());
    }

    public static void sendPubComp(ChannelHandlerContext channelHandlerContext, Integer messageId) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        channelHandlerContext.writeAndFlush(MqttMessageFactory.newMessage(mqttFixedHeader, mqttMessageIdVariableHeader, null));
    }

    public static void sendSubAck(ChannelHandlerContext channelHandlerContext, int subMessageId, MqttQoS[] qoses) {
        MqttSubAckMessage mqttSubAckMessage =
                MqttMessageBuilders.subAck().addGrantedQoses(qoses).packetId(subMessageId).build();
        channelHandlerContext.writeAndFlush(mqttSubAckMessage);
    }

    public static void sendUnSubAck(ChannelHandlerContext channelHandlerContext, int unSubMessageId) {
        MqttUnsubAckMessage unsubAckMessage = MqttMessageBuilders.unsubAck().packetId(unSubMessageId).build();
        channelHandlerContext.writeAndFlush(unsubAckMessage);
    }
}
