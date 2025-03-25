package com.ep.mqtt.server.rpc;

import com.ep.mqtt.server.metadata.DisconnectReason;
import com.ep.mqtt.server.metadata.RpcCommand;
import com.ep.mqtt.server.rpc.transfer.CheckRepeatSession;
import com.ep.mqtt.server.rpc.transfer.SendMessage;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.util.JsonUtil;
import com.ep.mqtt.server.util.MqttUtil;
import com.ep.mqtt.server.util.NettyUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;

/**
 * @author zbz
 * @date 2025/3/25 15:17
 */
public class RpcVerticle extends AbstractVerticle {

    private EventBus eventBus;

    @Override
    public void start() {
        eventBus = vertx.eventBus();

        // 发送消息
        eventBus.consumer(RpcCommand.SEND_MESSAGE.getCode(), message -> {
            SendMessage sendMessage = JsonUtil.string2Obj(String.valueOf(message.body()), SendMessage.class);
            if (sendMessage == null){
                return;
            }

            Session session = SessionManager.get(sendMessage.getToClientId());
            if (session != null){
                MqttUtil.sendPublish(session.getChannelHandlerContext(), sendMessage.getIsDup(), sendMessage.getSendQos(), sendMessage.getIsRetain(),
                        sendMessage.getTopic(), sendMessage.getSendPacketId(), sendMessage.getPayload());
            }
        });

        // 处理重复session
        eventBus.consumer(RpcCommand.CLEAN_EXIST_SESSION.getCode(), message -> {
            CheckRepeatSession checkRepeatSession = JsonUtil.string2Obj(String.valueOf(message.body()), CheckRepeatSession.class);
            if (checkRepeatSession == null){
                return;
            }

            Session existSession = SessionManager.get(checkRepeatSession.getClientId());
            if (existSession != null && !existSession.getSessionId().equals(checkRepeatSession.getSessionId())) {
                NettyUtil.setDisconnectReason(existSession.getChannelHandlerContext(), DisconnectReason.REPEAT_CONNECT);
                existSession.getChannelHandlerContext().disconnect();
            }
        });
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}