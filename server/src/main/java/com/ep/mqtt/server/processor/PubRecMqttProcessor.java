package com.ep.mqtt.server.processor;

import org.springframework.stereotype.Component;

import com.ep.mqtt.server.util.NettyUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

/**
 * 发布收到
 * 
 * @author zbz
 * @date 2023/7/31 9:27
 */
@Slf4j
@Component
public class PubRecMqttProcessor extends AbstractMqttProcessor<MqttMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) {
        inboundDeal.pubRec(channelHandlerContext, NettyUtil.getClientId(channelHandlerContext), getMessageId(mqttMessage));
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.PUBREC;
    }

    @Override
    protected MqttMessage castMqttMessage(MqttMessage mqttMessage) {
        return mqttMessage;
    }

}
