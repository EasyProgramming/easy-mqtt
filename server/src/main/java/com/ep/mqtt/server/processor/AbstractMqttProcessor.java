package com.ep.mqtt.server.processor;

import org.springframework.beans.factory.annotation.Autowired;

import com.ep.mqtt.server.deal.Deal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/7/14 16:10
 */
@Slf4j
public abstract class AbstractMqttProcessor<T extends MqttMessage> {

    @Autowired
    protected Deal deal;

    /**
     * 入口
     */
    public void begin(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) {
        log.debug("{}", castMqttMessage(mqttMessage).toString());
        process(channelHandlerContext, castMqttMessage(mqttMessage));
    }

    /**
     * 处理逻辑
     * 
     * @param channelHandlerContext
     *            netty处理队列上下文
     * @param mqttMessage
     *            netty解析的mqtt消息
     */
    protected abstract void process(ChannelHandlerContext channelHandlerContext, T mqttMessage);

    /**
     * 获取mqtt消息类型
     * 
     * @return mqtt消息类型
     */
    public abstract MqttMessageType getMqttMessageType();

    /**
     * 获取对应的mqtt消息
     * 
     * @param mqttMessage
     *            父类
     * @return 对应的mqtt消息
     */
    protected abstract T castMqttMessage(MqttMessage mqttMessage);

    /**
     * 获取消息id
     * 
     * @return 消息id
     */
    protected Integer getMessageId(MqttMessage mqttMessage) {
        if (mqttMessage.variableHeader() instanceof MqttMessageIdVariableHeader) {
            return ((MqttMessageIdVariableHeader)mqttMessage.variableHeader()).messageId();
        } else {
            throw new RuntimeException("非法的variableHeader");
        }
    }

}
