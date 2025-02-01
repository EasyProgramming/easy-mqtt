package com.ep.mqtt.server.processor;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.ep.mqtt.server.deal.DefaultDeal;
import com.ep.mqtt.server.metadata.BaseEnum;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.util.NettyUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 发布
 * 
 * @author : zbz
 * @date : 2023/7/26
 */
@Slf4j
@Component
public class PublishMqttProcessor extends AbstractMqttProcessor<MqttPublishMessage> {

    @Resource
    private DefaultDeal defaultDeal;

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttPublishMessage mqttPublishMessage) {
        byte[] data = new byte[mqttPublishMessage.payload().readableBytes()];
        mqttPublishMessage.payload().getBytes(mqttPublishMessage.payload().readerIndex(), data);
        String dataStr = new String(data);

        defaultDeal.publish(channelHandlerContext, BaseEnum.getByCode(mqttPublishMessage.fixedHeader().qosLevel(), Qos.class),
            mqttPublishMessage.variableHeader().topicName(), String.valueOf(mqttPublishMessage.variableHeader().packetId()),
            NettyUtil.getClientId(channelHandlerContext), dataStr, mqttPublishMessage.fixedHeader().isRetain());
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
