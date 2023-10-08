package com.ep.mqtt.server.listener;

import org.springframework.stereotype.Component;

import com.ep.mqtt.server.listener.msg.CleanExistSessionMsg;
import com.ep.mqtt.server.metadata.ChannelKey;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.util.JsonUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author : zbz
 * @date : 2023/8/18
 */
@Slf4j
@Component
public class CleanExistSessionListener extends AbstractListener<CleanExistSessionMsg> {

    @Override
    public void deal(CleanExistSessionMsg message) {
        Session session = SessionManager.get(message.getClientId());
        if (session != null && !session.getSessionId().equals(message.getSessionId())) {
            session.getChannelHandlerContext().disconnect();
        }
    }

    @Override
    public ChannelKey getChannelKey() {
        return ChannelKey.CLEAR_EXIST_SESSION;
    }

    @Override
    public CleanExistSessionMsg convertMessage(String messageStr) {
        return JsonUtil.string2Obj(messageStr, CleanExistSessionMsg.class);
    }

}
