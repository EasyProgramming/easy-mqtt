package com.ep.mqtt.server.processor;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ep.mqtt.server.util.NettyUtil;
import com.ep.mqtt.server.util.WorkerThreadPool;
import com.ep.mqtt.server.vo.TopicVo;
import com.google.common.collect.Lists;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;

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
        String clientId = NettyUtil.getClientId(channelHandlerContext);
        List<TopicVo> topicVoList = Lists.newArrayList();
        for (MqttTopicSubscription mqttTopicSubscription : mqttMessage.payload().topicSubscriptions()) {
            TopicVo topicVo = new TopicVo();
            topicVo.setQos(mqttTopicSubscription.qualityOfService().value());
            topicVo.setTopicFilter(mqttTopicSubscription.topicName());
            topicVoList.add(topicVo);
        }
        List<Integer> subscribeResultList = deal.subscribe(clientId, topicVoList);
        List<TopicVo> successSubscribeTopicList = Lists.newArrayList();
        List<MqttQoS> mqttQosList = Lists.newArrayList();
        for (int i = 0; i < subscribeResultList.size(); i++) {
            Integer qosValue = subscribeResultList.get(i);
            mqttQosList.add(MqttQoS.valueOf(qosValue));
            if (MqttQoS.FAILURE.value() != qosValue) {
                successSubscribeTopicList.add(topicVoList.get(i));
            }
        }
        MqttQoS[] qoses = {};
        mqttQosList.toArray(qoses);
        int subMessageId = mqttMessage.variableHeader().messageId();
        MqttSubAckMessage mqttSubAckMessage =
            MqttMessageBuilders.subAck().addGrantedQoses(qoses).packetId(subMessageId).build();
        channelHandlerContext.writeAndFlush(mqttSubAckMessage);
        WorkerThreadPool.execute(event -> deal.sendTopicRetainMessage(clientId, successSubscribeTopicList));
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
