package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.metadata.BaseEnum;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;
import com.ep.mqtt.server.util.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 发布
 * 
 * @author : zbz
 * @date : 2023/7/26
 */
@Slf4j
@Component
public class PublishMqttProcessor extends AbstractMqttProcessor<MqttPublishMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttPublishMessage mqttPublishMessage) {
        byte[] data = new byte[mqttPublishMessage.payload().readableBytes()];
        mqttPublishMessage.payload().getBytes(mqttPublishMessage.payload().readerIndex(), data);
        String dataStr = new String(data);

        inboundDeal.publish(channelHandlerContext, BaseEnum.getByCode(mqttPublishMessage.fixedHeader().qosLevel().value(), Qos.class),
            mqttPublishMessage.variableHeader().topicName(), mqttPublishMessage.variableHeader().packetId(),
            NettyUtil.getClientId(channelHandlerContext), dataStr, mqttPublishMessage.fixedHeader().isRetain() ? YesOrNo.YES : YesOrNo.NO);
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.PUBLISH;
    }

    @Override
    protected MqttPublishMessage castMqttMessage(MqttMessage mqttMessage) {
        return (MqttPublishMessage)mqttMessage;
    }

}
