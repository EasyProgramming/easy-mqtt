package com.ep.mqtt.server.processor;

import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

/**
 * 断开连接
 * 
 * @author zbz
 * @date 2023/7/26 16:47
 */
@Slf4j
@Component
public class DisconnectMqttProcessor extends AbstractMqttProcessor<MqttMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) {
        channelHandlerContext.disconnect();
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.DISCONNECT;
    }

    @Override
    protected MqttMessage castMqttMessage(MqttMessage mqttMessage) {
        return mqttMessage;
    }

}
