package com.ep.mqtt.server.aliyun;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.UnsupportedEncodingException;

/**
 * @author zbz
 * @date 2023/11/6 14:58
 */
@ConditionalOnProperty(prefix = "mqtt.server.aliyun.data-transfer.rocket-mq", value = "name-server")
public class RocketMqProducer {

    private final DefaultMQProducer defaultProducer;

    public RocketMqProducer(String producerGroup, String nameServer) throws MQClientException {
        defaultProducer = new DefaultMQProducer(producerGroup);
        defaultProducer.setNamesrvAddr(nameServer);
        defaultProducer.start();
    }

    public void destroy(){
        defaultProducer.shutdown();
    }

    public SendResult send(String topic, String body) throws UnsupportedEncodingException, InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Message msg = new Message(topic, "*", body.getBytes(RemotingHelper.DEFAULT_CHARSET));
        return defaultProducer.send(msg);
    }

}
