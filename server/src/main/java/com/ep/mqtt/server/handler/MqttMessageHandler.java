package com.ep.mqtt.server.handler;

import com.ep.mqtt.server.deal.InboundDeal;
import com.ep.mqtt.server.processor.AbstractMqttProcessor;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.util.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zbz
 * @date 2023/7/1 14:15
 */
@Slf4j
public class MqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private final InboundDeal inboundDeal;

    private final Map<MqttMessageType, AbstractMqttProcessor<?>> abstractMqttProcessorMap;

    public MqttMessageHandler(List<AbstractMqttProcessor<?>> abstractMqttProcessorList, InboundDeal inboundDeal) {
        abstractMqttProcessorMap = abstractMqttProcessorList.stream()
            .collect(Collectors.toMap(AbstractMqttProcessor::getMqttMessageType, b -> b));
        this.inboundDeal = inboundDeal;
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

        NettyUtil.setCleanDataReason(ctx, "occur exception");
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent)evt;
            if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                NettyUtil.setCleanDataReason(ctx, "ping timeout");

                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 连接断开的事件 <br/>
     * 为什么不在这里清理客户端的数据：1-耗费性能 2-会在客户端重连时进行清理（如果长时间未重连，按过期数据清理）
     * 
     * @param ctx
     *            上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String sessionId = NettyUtil.getSessionId(ctx);
        String clientId = NettyUtil.getClientId(ctx);

        log.info("client session id: [{}], client id: [{}] inactive", sessionId, clientId);
        if (StringUtils.isBlank(clientId)) {
            return;
        }

        Session session = SessionManager.get(clientId);

        SessionManager.unbind(clientId);
        String cleanDataReason = NettyUtil.getCleanDataReason(ctx);
        if (StringUtils.isNotBlank(cleanDataReason)){
            log.info("client session id: [{}], client id: [{}], clear data reason[{}]", sessionId, clientId, cleanDataReason);

            if (session!= null && session.getIsCleanSession()){
                inboundDeal.clearClientData(clientId);
            }
        }

        // TODO: 2025/1/1 获取这个客户端的遗嘱消息，并发送
    }

}
