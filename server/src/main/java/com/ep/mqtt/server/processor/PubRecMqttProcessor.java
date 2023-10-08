package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.util.MqttUtil;
import com.ep.mqtt.server.util.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        Integer messageId = getMessageId(mqttMessage);
        String clientId = NettyUtil.getClientId(channelHandlerContext);
        defaultDeal.delMessage(clientId, messageId);
        defaultDeal.saveRelMessage(clientId, messageId);
        MqttUtil.sendPubRel(channelHandlerContext, messageId);
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
