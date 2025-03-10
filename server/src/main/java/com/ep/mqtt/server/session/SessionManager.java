package com.ep.mqtt.server.session;

import com.ep.mqtt.server.metadata.DisconnectReason;
import com.ep.mqtt.server.util.NettyUtil;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author zbz
 * @date 2023/7/16 16:38
 */
public class SessionManager {

    private static final Map<String, Session> SESSION_MAP = Maps.newConcurrentMap();

    public static void bind(String clientId, Session session) {
        synchronized (clientId.intern()){
            Session existSession = SESSION_MAP.get(clientId);

            if (existSession != null && !existSession.getSessionId().equals(session.getSessionId())){
                NettyUtil.setDisconnectReason(existSession.getChannelHandlerContext(), DisconnectReason.REPEAT_CONNECT);
                existSession.getChannelHandlerContext().disconnect();
            }

            NettyUtil.setClientId(session.getChannelHandlerContext(), clientId);
            SESSION_MAP.put(clientId, session);
        }
    }

    public static void unbind(String clientId, String sessionId) {
        synchronized (clientId.intern()){
            Session existSession = SESSION_MAP.get(clientId);
            if (existSession == null){
                return;
            }

            if (!existSession.getSessionId().equals(sessionId)){
                return;
            }

            SESSION_MAP.remove(clientId);
        }
    }

    public static Session get(String clientId) {
        return SESSION_MAP.get(clientId);
    }

}
