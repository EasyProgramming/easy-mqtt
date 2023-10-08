package com.ep.mqtt.server.processor;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ep.mqtt.server.util.NettyUtil;
import com.ep.mqtt.server.vo.TopicVo;
import com.google.common.collect.Lists;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;

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
        String clientId = NettyUtil.getClientId(channelHandlerContext);
        List<TopicVo> topicVoList = Lists.newArrayList();
        for (String topicFilter : mqttMessage.payload().topics()) {
            TopicVo topicVo = new TopicVo();
            topicVo.setTopicFilter(topicFilter);
            topicVoList.add(topicVo);
        }
        defaultDeal.unSubscribe(clientId, topicVoList);
        // 发送取消订阅确认消息
        int subMessageId = mqttMessage.variableHeader().messageId();
        MqttUnsubAckMessage unsubAckMessage = MqttMessageBuilders.unsubAck().packetId(subMessageId).build();
        channelHandlerContext.writeAndFlush(unsubAckMessage);
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
