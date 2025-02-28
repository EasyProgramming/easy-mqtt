package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.util.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订阅
 * 
 * @author zbz
 * @date 2023/7/19 11:44
 */
@Slf4j
@Component
public class SubscribeMqttProcessor extends AbstractMqttProcessor<MqttSubscribeMessage> {

    @Override
    public void process(ChannelHandlerContext channelHandlerContext, MqttSubscribeMessage mqttMessage) {
        inboundDeal.subscribe(channelHandlerContext,mqttMessage.variableHeader().messageId(), NettyUtil.getClientId(channelHandlerContext),
                mqttMessage.payload().topicSubscriptions());
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.SUBSCRIBE;
    }

    @Override
    protected MqttSubscribeMessage castMqttMessage(MqttMessage mqttMessage) {
        return (MqttSubscribeMessage)mqttMessage;
    }
}
