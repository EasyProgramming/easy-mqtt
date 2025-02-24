package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.util.NettyUtil;
import com.google.common.collect.Sets;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 取消订阅
 * 
 * @author zbz
 * @date 2023/7/26 16:26
 */
@Slf4j
@Component
public class UnSubscribeMqttProcessor extends AbstractMqttProcessor<MqttUnsubscribeMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttUnsubscribeMessage mqttMessage) {
        defaultDeal.unSubscribe(channelHandlerContext, mqttMessage.variableHeader().messageId(), NettyUtil.getClientId(channelHandlerContext),
                Sets.newHashSet(mqttMessage.payload().topics()));
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.UNSUBSCRIBE;
    }

    @Override
    protected MqttUnsubscribeMessage castMqttMessage(MqttMessage mqttMessage) {
        return (MqttUnsubscribeMessage)mqttMessage;
    }

}
