package com.ep.mqtt.server.processor;

import org.springframework.stereotype.Component;

import com.ep.mqtt.server.util.NettyUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

/**
 * 发布释放
 * 
 * @author zbz
 * @date 2023/7/31 9:47
 */
@Slf4j
@Component
public class PubRelMqttProcessor extends AbstractMqttProcessor<MqttMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) {
        defaultDeal.pubRel(channelHandlerContext, getMessageId(mqttMessage), NettyUtil.getClientId(channelHandlerContext));
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.PUBREL;
    }

    @Override
    protected MqttMessage castMqttMessage(MqttMessage mqttMessage) {
        return mqttMessage;
    }

}
