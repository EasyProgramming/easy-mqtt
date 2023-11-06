package com.ep.mqtt.server.aliyun;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * @author zbz
 * @date 2023/11/6 16:48
 */
@Slf4j
public class DataInputProcessor {

    private final List<DefaultMQPushConsumer> consumerList = Lists.newArrayList();

    public DataInputProcessor(List<MqttServerProperties.Aliyun.TopicMappingRule> inputRuleList, String nameServer) throws MQClientException {
        for (MqttServerProperties.Aliyun.TopicMappingRule topicMappingRule : inputRuleList){
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("easy-mqtt");
            consumer.setNamesrvAddr(nameServer);
            consumer.subscribe(topicMappingRule.getFromTopic(), "*");
            consumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
                try {
                    dealMsg(msgList, topicMappingRule);
                }
                catch (Throwable e){
                    log.warn("consumer error", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumerList.add(consumer);
        }
    }

    public void start() throws MQClientException {
        for (DefaultMQPushConsumer consumer : consumerList){
            consumer.start();
        }
    }

    public void shutdown(){
        for (DefaultMQPushConsumer consumer : consumerList){
            consumer.shutdown();
        }
    }

    private void dealMsg(List<MessageExt> msgList, MqttServerProperties.Aliyun.TopicMappingRule topicMappingRule){
        for (MessageExt msg : msgList){
            // 转发消息，应该是直接sendMessage

            //

        }
    }
}
