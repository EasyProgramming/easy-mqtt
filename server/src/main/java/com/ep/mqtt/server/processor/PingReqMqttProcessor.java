package com.ep.mqtt.server.processor;

import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/7/26 17:39
 */
@Slf4j
@Component
public class PingReqMqttProcessor extends AbstractMqttProcessor<MqttMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) {
        channelHandlerContext.writeAndFlush(MqttMessage.PINGRESP);
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.PINGREQ;
    }

    @Override
    protected MqttMessage castMqttMessage(MqttMessage mqttMessage) {
        return mqttMessage;
    }

}
