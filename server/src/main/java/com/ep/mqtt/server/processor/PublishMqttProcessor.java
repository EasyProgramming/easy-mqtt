package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.metadata.YesOrNo;
import com.ep.mqtt.server.util.MqttUtil;
import com.ep.mqtt.server.util.NettyUtil;
import com.ep.mqtt.server.util.WorkerThreadPool;
import com.ep.mqtt.server.vo.MessageVo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        MessageVo messageVo = convert(mqttPublishMessage, channelHandlerContext);
        WorkerThreadPool.dealMessage((a)-> dealMessage(messageVo), ()->{
            switch (mqttPublishMessage.fixedHeader().qosLevel()) {
                case AT_LEAST_ONCE:
                    MqttMessage publishAckMessage =
                            MqttMessageBuilders.pubAck().packetId(messageVo.getFromMessageId()).build();
                    channelHandlerContext.writeAndFlush(publishAckMessage);
                    break;
                case EXACTLY_ONCE:
                    MqttUtil.sendPubRec(channelHandlerContext, messageVo.getFromMessageId());
                    break;
                default:
                    break;
            }
        }, channelHandlerContext);
    }

    private void dealMessage(MessageVo messageVo) {
        Integer isRetain = messageVo.getIsRetained();
        MqttQoS fromMqttQoS = MqttQoS.valueOf(messageVo.getFromQos());
        String payload = messageVo.getPayload();
        if (YesOrNo.YES.getValue().equals(isRetain)) {
            // qos == 0 || payload 为零字节，清除该主题下的保留消息
            if (MqttQoS.AT_MOST_ONCE == fromMqttQoS || StringUtils.isBlank(payload)) {
                deal.delTopicRetainMessage(messageVo.getTopic());
            }
            // 存储保留消息
            else {
                deal.saveTopicRetainMessage(messageVo);
            }
        }
        if (MqttQoS.EXACTLY_ONCE.equals(fromMqttQoS)) {
            deal.saveRecMessage(messageVo);
            return;
        }
        deal.sendMessage(messageVo);
    }

    private MessageVo convert(MqttPublishMessage mqttPublishMessage, ChannelHandlerContext channelHandlerContext) {
        MessageVo messageVo = new MessageVo();
        messageVo.setFromClientId(NettyUtil.getClientId(channelHandlerContext));
        messageVo.setFromMessageId(mqttPublishMessage.variableHeader().packetId());
        messageVo.setIsRetained(YesOrNo.valueOf(mqttPublishMessage.fixedHeader().isRetain()).getValue());
        byte[] data = new byte[mqttPublishMessage.payload().readableBytes()];
        mqttPublishMessage.payload().getBytes(mqttPublishMessage.payload().readerIndex(), data);
        messageVo.setPayload(new String(data));
        messageVo.setFromQos(mqttPublishMessage.fixedHeader().qosLevel().value());
        messageVo.setTopic(mqttPublishMessage.variableHeader().topicName());
        messageVo.setIsDup(false);
        return messageVo;
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
