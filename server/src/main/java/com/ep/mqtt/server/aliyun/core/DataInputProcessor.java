package com.ep.mqtt.server.aliyun.core;

import com.aliyun.openservices.ons.api.*;
import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.deal.Deal;
import com.ep.mqtt.server.metadata.RocketMqMessagePropertyKey;
import com.ep.mqtt.server.metadata.RocketMqMessageType;
import com.ep.mqtt.server.metadata.YesOrNo;
import com.ep.mqtt.server.vo.MessageVo;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * @author zbz
 * @date 2023/11/6 16:48
 */
@Slf4j
public class DataInputProcessor {

    private final List<Consumer> consumerList = Lists.newArrayList();

    private final Deal deal;

    public DataInputProcessor(List<MqttServerProperties.Aliyun.TopicMapRule> inputRuleList, MqttServerProperties.Aliyun.RocketMq rocketMq, Deal deal){
        this.deal = deal;
        for (MqttServerProperties.Aliyun.TopicMapRule topicMapRule : inputRuleList){
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.GROUP_ID, rocketMq.getGroupId());
            properties.put(PropertyKeyConst.AccessKey, rocketMq.getAccessKey());
            properties.put(PropertyKeyConst.SecretKey, rocketMq.getSecretKey());
            properties.put(PropertyKeyConst.NAMESRV_ADDR, rocketMq.getNameserverAddr());
            MqttServerProperties.Aliyun.RocketMqTopic rocketMqTopic = topicMapRule.getRocketMqTopic();
            if (rocketMqTopic == null) {
                continue;
            }
            if (RocketMqMessageType.NORMAL.getKey().equals(rocketMqTopic.getMessageType())) {
                Consumer consumer = ONSFactory.createConsumer(properties);
                consumer.subscribe(rocketMqTopic.getTopic(), "*", (message, context) -> {
                    dealMsg(message, topicMapRule);
                    return Action.CommitMessage;
                });
                consumerList.add(consumer);
            }
            else {
                log.warn("un support rocketmq message type");
            }
        }
    }

    public void start() {
        for (Consumer consumer : consumerList){
            consumer.start();
        }
    }

    public void shutdown(){
        for (Consumer consumer : consumerList){
            consumer.shutdown();
        }
    }

    private void dealMsg(Message msg, MqttServerProperties.Aliyun.TopicMapRule topicMapRule){
        MessageVo messageVo = convert(msg, topicMapRule);
        deal.sendMessage(messageVo);
    }

    private MessageVo convert(Message msg, MqttServerProperties.Aliyun.TopicMapRule topicMapRule){
        MessageVo messageVo = new MessageVo();
        messageVo.setIsRetained(YesOrNo.NO.getValue());
        messageVo.setIsDup(false);
        String qosStr = msg.getUserProperties(RocketMqMessagePropertyKey.QOS_LEVEL.getKey());
        if (StringUtils.isNotBlank(qosStr)){
            messageVo.setFromQos(Integer.valueOf(qosStr));
        }
        else {
            messageVo.setFromQos(1);
        }
        String topic = topicMapRule.getMqttTopic().getTopic();
        String subTopic = msg.getUserProperties(RocketMqMessagePropertyKey.MQTT_SECOND_TOPIC.getKey());
        if (StringUtils.isNotBlank(subTopic)){
            topic = topic + subTopic;
        }
        messageVo.setTopic(topic);
        messageVo.setPayload(new String(msg.getBody(), StandardCharsets.UTF_8));
        return messageVo;
    }
}
