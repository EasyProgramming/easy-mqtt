package com.ep.mqtt.server.listener;

import com.ep.mqtt.server.metadata.ChannelKey;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.session.SessionManager;
import com.ep.mqtt.server.util.JsonUtil;
import com.ep.mqtt.server.util.MqttUtil;
import com.ep.mqtt.server.vo.MessageVo;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author : zbz
 * @date : 2023/8/18
 */
@Slf4j
@Component
public class SendMessageListener extends AbstractListener<List<MessageVo>> {

    @Override
    public void deal(List<MessageVo> messageVoList) {
        if (CollectionUtils.isEmpty(messageVoList)) {
            return;
        }
        for (MessageVo messageVo : messageVoList){
            Session session = SessionManager.get(messageVo.getToClientId());
            if (session != null) {
                MqttUtil.sendPublish(session.getChannelHandlerContext(), messageVo);
            }
        }
    }

    @Override
    public ChannelKey getChannelKey() {
        return ChannelKey.SEND_MESSAGE;
    }

    @Override
    public List<MessageVo> convertMessage(String messageStr) {
        return JsonUtil.string2Obj(messageStr, new TypeReference<List<MessageVo>>() {});
    }

}
