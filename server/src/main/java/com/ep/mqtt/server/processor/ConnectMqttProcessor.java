package com.ep.mqtt.server.processor;

import com.ep.mqtt.server.db.dto.ClientDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.util.NettyUtil;
import com.ep.mqtt.server.vo.ClientInfoVo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

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
            if (!defaultDeal.authentication(mqttConnectMessage)) {
                // 认证失败，返回错误的ack消息
                sendConnectAck(channelHandlerContext,
                    MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, false, true);
                return;
            }

            // 设置心跳时间
            int keepAliveTimeSeconds = keepAlive(channelHandlerContext, mqttConnectMessage);

            ClientDto clientDto = defaultDeal.getClientInfo(clientIdentifier);
            boolean isCleanSession = mqttConnectMessage.variableHeader().isCleanSession();
            boolean sessionPresent = false;
            if (clientDto != null) {
                if (isCleanSession) {
                    // 清除之前的数据
                    defaultDeal.clearClientData(clientIdentifier);
                } else {
                    defaultDeal.reConnect(clientInfo, channelHandlerContext);
                    sessionPresent = true;
                }
            } else {
                if (!isCleanSession) {
                    // 持久化会话
                    clientInfo = new ClientInfoVo();
                    clientInfo.setClientId(clientIdentifier);
                    clientInfo.setConnectTime(System.currentTimeMillis());
                    defaultDeal.saveClientInfo(clientInfo);
                }
            }

            // 新建内存会话
            Session session = new Session();
            session.setIsCleanSession(isCleanSession);
            session.setClientId(clientIdentifier);
            session.setChannelHandlerContext(channelHandlerContext);
            session.setSessionId(NettyUtil.getSessionId(channelHandlerContext));
            session.setKeepAliveTimeSeconds(keepAliveTimeSeconds);
            SessionManager.bind(clientIdentifier, session);
            // 踢出重复会话
            defaultDeal.cleanExistSession(clientIdentifier, session.getSessionId());
            // 刷新数据
            defaultDeal.refreshData(session);
            sendConnectAck(channelHandlerContext, MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent, false);
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
