package com.ep.mqtt.server.deal;

import com.ep.mqtt.server.vo.MessageVo;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/11/9 15:08
 */
@Slf4j
public class DefaultDeal extends Deal {

    @Override
    public void pubRel(Integer messageId, String clientId){
        MessageVo messageVo = getRecMessage(clientId, messageId);
        if (messageVo == null) {
            return;
        }
        sendMessage(messageVo);
        delRecMessage(clientId, messageId);
    }

}
