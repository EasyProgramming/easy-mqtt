package com.ep.mqtt.server.aliyun.core;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.deal.Deal;
import com.ep.mqtt.server.metadata.RocketMqMessagePropertyKey;
import com.ep.mqtt.server.util.TopicUtil;
import com.ep.mqtt.server.vo.MessageVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.Properties;

/**
 * @author zbz
 * @date 2023/11/9 14:59
 */
@Slf4j
public class AliyunDeal extends Deal {

    private static final Integer P2P_PARAMS_SIZE = 3;

    private static final String P2P_FLAG = "/p2p/";

    @Autowired
    private RocketMqProducer rocketMqProducer;

    @Override
    public void sendMessage(MessageVo messageVo) {
        boolean isP2P = false;
        String[] splitResult = StringUtils.split(messageVo.getTopic(), P2P_FLAG);
        if (splitResult.length == P2P_PARAMS_SIZE){
            if (!StringUtils.containsAny(splitResult[0], TopicUtil.TOPIC_SPLIT_FLAG)){
                isP2P = true;
                messageVo.setTopic(splitResult[0]);
                messageVo.setToClientId(splitResult[2]);
            }
        }
        super.sendMessage(messageVo, isP2P);
    }

    @Override
    public void publish(MessageVo messageVo) {
        String outputTopic = getOutputTopic(messageVo.getTopic());
        if (StringUtils.isBlank(outputTopic)){
            super.publish(messageVo);
            return;
        }
        Properties properties = new Properties();
        properties.setProperty(RocketMqMessagePropertyKey.QOS_LEVEL.getKey(), String.valueOf(messageVo.getFromQos()));
        // 因为rmq和mqtt的topic是全匹配，且rmq的topic不含有/，所以不会有子级的topic
        properties.setProperty(RocketMqMessagePropertyKey.MQTT_SECOND_TOPIC.getKey(), "");
        properties.setProperty(RocketMqMessagePropertyKey.CLIENT_ID.getKey(), messageVo.getFromClientId());
        rocketMqProducer.send(outputTopic, messageVo.getPayload(), properties);
    }

    private String getOutputTopic(String mqttTopic){
        if (mqttServerProperties.getAliyun().getDataTransfer() == null){
            return "";
        }
        if (CollectionUtils.isEmpty(mqttServerProperties.getAliyun().getDataTransfer().getOutputRuleList())){
            return "";
        }
        for (MqttServerProperties.Aliyun.TopicMapRule topicMapRule : mqttServerProperties.getAliyun().getDataTransfer().getOutputRuleList()){
            if (topicMapRule.getRocketMqTopic().getTopic().equals(mqttTopic)){
                return topicMapRule.getMqttTopic().getTopic();
            }
        }
        return "";
    }

}
