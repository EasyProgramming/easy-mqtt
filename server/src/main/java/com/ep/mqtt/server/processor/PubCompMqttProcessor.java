package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.util.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author zbz
 * @date 2023/7/31 15:43
 */
@Slf4j
@Component
public class PubCompMqttProcessor extends AbstractMqttProcessor<MqttMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) {
        Integer messageId = getMessageId(mqttMessage);
        String clientId = NettyUtil.getClientId(channelHandlerContext);
        defaultDeal.delRelMessage(clientId, messageId);
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
