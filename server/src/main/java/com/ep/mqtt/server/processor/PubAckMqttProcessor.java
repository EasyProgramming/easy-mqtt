package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.util.NettyUtil;
import com.ep.mqtt.server.util.WorkerThreadPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        WorkerThreadPool.dealMessage((a)-> inboundDeal.delMessage(NettyUtil.getClientId(channelHandlerContext),
                mqttPubAckMessage.variableHeader().messageId()), null, channelHandlerContext);
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
