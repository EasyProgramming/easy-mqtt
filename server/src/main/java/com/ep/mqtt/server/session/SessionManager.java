package com.ep.mqtt.server.session;

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
        NettyUtil.setClientId(session.getChannelHandlerContext(), clientId);
        SESSION_MAP.put(clientId, session);
    }

    public static void unbind(String clientId) {
        SESSION_MAP.remove(clientId);
    }

    public static Session get(String clientId) {
        return SESSION_MAP.get(clientId);
    }

}
