package com.ep.mqtt.server.handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.ep.mqtt.server.deal.DefaultDeal;
import com.ep.mqtt.server.processor.AbstractMqttProcessor;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.util.NettyUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/7/1 14:15
 */
@Slf4j
public class MqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private final DefaultDeal defaultDeal;

    private final Map<MqttMessageType, AbstractMqttProcessor<?>> abstractMqttProcessorMap;

    public MqttMessageHandler(List<AbstractMqttProcessor<?>> abstractMqttProcessorList, DefaultDeal defaultDeal) {
        abstractMqttProcessorMap = abstractMqttProcessorList.stream()
            .collect(Collectors.toMap(AbstractMqttProcessor::getMqttMessageType, b -> b));
        this.defaultDeal = defaultDeal;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
        AbstractMqttProcessor<?> abstractMqttProcessor = abstractMqttProcessorMap.get(msg.fixedHeader().messageType());
        if (abstractMqttProcessor == null) {
            throw new RuntimeException("不支持的报文");
        }
        abstractMqttProcessor.begin(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client session id: [{}], client id: [{}] occurred error", NettyUtil.getSessionId(ctx),
            NettyUtil.getClientId(ctx), cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent)evt;
            if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("client session id: [{}], client id: [{}] inactive", NettyUtil.getSessionId(ctx),
            NettyUtil.getClientId(ctx));
        String clientId = NettyUtil.getClientId(ctx);

        if (StringUtils.isBlank(clientId)) {
            return;
        }

        SessionManager.unbind(clientId);

        // TODO: 2025/1/1 获取这个客户端的遗嘱消息，并发送
    }

}
