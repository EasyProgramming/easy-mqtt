package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.util.NettyUtil;
import com.ep.mqtt.server.util.WorkerThreadPool;
import com.ep.mqtt.server.vo.MessageVo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 发布释放
 * 
 * @author zbz
 * @date 2023/7/31 9:47
 */
@Slf4j
@Component
public class PubRelMqttProcessor extends AbstractMqttProcessor<MqttMessage> {

    @Override
    protected void process(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) {
        Integer messageId = getMessageId(mqttMessage);
        String clientId = NettyUtil.getClientId(channelHandlerContext);
        WorkerThreadPool.dealMessage((a)-> {
            deal.pubRel(messageId, clientId);
        }, ()-> sendPubComp(channelHandlerContext, messageId), channelHandlerContext);
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.PUBREL;
    }

    @Override
    protected MqttMessage castMqttMessage(MqttMessage mqttMessage) {
        return mqttMessage;
    }

    private void sendPubComp(ChannelHandlerContext channelHandlerContext, Integer messageId) {
        MqttFixedHeader mqttFixedHeader =
            new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        channelHandlerContext
            .writeAndFlush(MqttMessageFactory.newMessage(mqttFixedHeader, mqttMessageIdVariableHeader, null));
    }

}
