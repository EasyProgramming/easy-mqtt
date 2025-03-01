package com.ep.mqtt.server.processor;

import org.springframework.stereotype.Component;

import com.ep.mqtt.server.util.NettyUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 发布确认
 * 
 * @author zbz
 * @date 2023/7/29 14:18
 */
@Slf4j
@Component
public class PubAckMqttProcessor extends AbstractMqttProcessor<MqttPubAckMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttPubAckMessage mqttPubAckMessage) {
        inboundDeal.pubAck(NettyUtil.getClientId(channelHandlerContext), mqttPubAckMessage.variableHeader().messageId());
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.PUBACK;
    }

    @Override
    protected MqttPubAckMessage castMqttMessage(MqttMessage mqttMessage) {
        return (MqttPubAckMessage)mqttMessage;
    }

}
