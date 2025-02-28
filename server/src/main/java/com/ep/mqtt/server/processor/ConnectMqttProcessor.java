package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.metadata.RaftCommand;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.ep.mqtt.server.raft.transfer.CheckRepeatSession;
import com.ep.mqtt.server.raft.transfer.TransferData;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.util.JsonUtil;
import com.ep.mqtt.server.util.NettyUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 建立连接
 * 
 * @author zbz
 * @date 2023/7/14 16:42
 */
@Slf4j
@Component
public class ConnectMqttProcessor extends AbstractMqttProcessor<MqttConnectMessage> {

    private static final int MIN_KEEP_ALIVE_TIME_SECONDS = 30;

    @Override
    public void process(ChannelHandlerContext channelHandlerContext, MqttConnectMessage mqttConnectMessage) {
        try {
            // 判断协议版本
            String clientIdentifier = mqttConnectMessage.payload().clientIdentifier();
            if (!validVersion(mqttConnectMessage.variableHeader().version())) {
                sendConnectAck(channelHandlerContext,
                    MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false, true);
                return;
            }

            // 判断协议是否合法
            if (StringUtils.isBlank(clientIdentifier)) {
                sendConnectAck(channelHandlerContext, MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED,
                    false, true);
                return;
            }

            // 鉴权
            if (!inboundDeal.authentication(mqttConnectMessage)) {
                // 认证失败，返回错误的ack消息
                sendConnectAck(channelHandlerContext,
                    MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, false, true);
                return;
            }

            // 设置心跳时间
            int keepAliveTimeSeconds = keepAlive(channelHandlerContext, mqttConnectMessage);

            boolean isCleanSession = mqttConnectMessage.variableHeader().isCleanSession();

            inboundDeal.connect(clientIdentifier, isCleanSession);

            // 新建内存会话
            Session session = new Session();
            session.setIsCleanSession(isCleanSession);
            session.setClientId(clientIdentifier);
            session.setChannelHandlerContext(channelHandlerContext);
            session.setSessionId(NettyUtil.getSessionId(channelHandlerContext));
            session.setKeepAliveTimeSeconds(keepAliveTimeSeconds);
            SessionManager.bind(clientIdentifier, session);

            // 踢出重复会话
            CheckRepeatSession checkRepeatSession = new CheckRepeatSession();
            checkRepeatSession.setSessionId(session.getSessionId());
            checkRepeatSession.setClientId(session.getClientId());
            EasyMqttRaftClient.syncSend(JsonUtil.obj2String(
                new TransferData(RaftCommand.CLEAN_EXIST_SESSION, JsonUtil.obj2String(checkRepeatSession))));

            sendConnectAck(channelHandlerContext, MqttConnectReturnCode.CONNECTION_ACCEPTED, !isCleanSession, false);
            log.info("client session id: [{}], client id: [{}] connect", session.getSessionId(), session.getClientId());
        } catch (Throwable throwable) {
            log.error("mqtt connect message process error", throwable);
            sendConnectAck(channelHandlerContext, MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, false,
                true);
        }
    }

    private int keepAlive(ChannelHandlerContext channelHandlerContext, MqttConnectMessage mqttConnectMessage) {
        int keepAliveTimeSeconds = mqttConnectMessage.variableHeader().keepAliveTimeSeconds();
        if (keepAliveTimeSeconds <= MIN_KEEP_ALIVE_TIME_SECONDS) {
            keepAliveTimeSeconds = MIN_KEEP_ALIVE_TIME_SECONDS;
        }
        channelHandlerContext.pipeline().addFirst("idle",
            new IdleStateHandler(0, 0, (int)(keepAliveTimeSeconds * 1.5f)));
        return keepAliveTimeSeconds;
    }

    private void sendConnectAck(ChannelHandlerContext channelHandlerContext, MqttConnectReturnCode returnCode,
        boolean sessionPresent, Boolean isCloseConnection) {
        MqttConnAckMessage ackMessage =
            MqttMessageBuilders.connAck().sessionPresent(sessionPresent).returnCode(returnCode).build();
        channelHandlerContext.writeAndFlush(ackMessage);
        if (isCloseConnection) {
            channelHandlerContext.disconnect();
        }
    }

    @Override
    public MqttMessageType getMqttMessageType() {
        return MqttMessageType.CONNECT;
    }

    @Override
    protected MqttConnectMessage castMqttMessage(MqttMessage mqttMessage) {
        return (MqttConnectMessage)mqttMessage;
    }

    private boolean validVersion(int mqttVersion) {
        return MqttVersion.MQTT_3_1.protocolLevel() == mqttVersion
            || MqttVersion.MQTT_3_1_1.protocolLevel() == mqttVersion;
    }
}
