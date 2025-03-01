package com.ep.mqtt.server.processor;

import org.springframework.stereotype.Component;

import com.ep.mqtt.server.util.NettyUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/7/31 15:43
 */
@Slf4j
@Component
public class PubCompMqttProcessor extends AbstractMqttProcessor<MqttMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) {
        inboundDeal.pubComp(NettyUtil.getClientId(channelHandlerContext), getMessageId(mqttMessage));
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.PUBCOMP;
    }

    @Override
    protected MqttMessage castMqttMessage(MqttMessage mqttMessage) {
        return mqttMessage;
    }

}
