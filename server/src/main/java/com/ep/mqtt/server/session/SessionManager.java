package com.ep.mqtt.server.session;

import com.ep.mqtt.server.deal.MessageIdDeal;
import com.ep.mqtt.server.metadata.DisconnectReason;
import com.ep.mqtt.server.metadata.LocalLock;
import com.ep.mqtt.server.util.NettyUtil;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author zbz
 * @date 2023/7/16 16:38
 */
public class SessionManager {

    private static final Map<String, Session> SESSION_MAP = Maps.newConcurrentMap();

    private static final Map<String, Session> SESSION_MAP_2 = Maps.newConcurrentMap();

    public static void bind(String clientId, Session session) {
        synchronized (LocalLock.LOCK_CLIENT.getLocalLockName(clientId)){
            Session existSession = SESSION_MAP.get(clientId);

            if (existSession != null && !existSession.getSessionId().equals(session.getSessionId())){
                NettyUtil.setDisconnectReason(existSession.getChannelHandlerContext(), DisconnectReason.REPEAT_CONNECT);
                existSession.getChannelHandlerContext().disconnect();
            }

            NettyUtil.setClientId(session.getChannelHandlerContext(), clientId);
            SESSION_MAP.put(clientId, session);

            if (SESSION_MAP_2.get(session.getSessionId()) != null){
                throw new RuntimeException(String.format("repeat session id [%s]", session.getSessionId()));
            }
            SESSION_MAP_2.put(session.getSessionId(), session);
        }
    }

    public static void unbind(String clientId, String sessionId) {
        synchronized (LocalLock.LOCK_CLIENT.getLocalLockName(clientId)){
            SESSION_MAP_2.remove(sessionId);

            MessageIdDeal.remove(clientId);

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

    public static Session getBySessionId(String sessionId) {
        return SESSION_MAP_2.get(sessionId);
    }
}
