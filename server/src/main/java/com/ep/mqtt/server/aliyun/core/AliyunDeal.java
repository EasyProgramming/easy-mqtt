package com.ep.mqtt.server.aliyun.core;

import com.ep.mqtt.server.deal.Deal;
import com.ep.mqtt.server.vo.MessageVo;
import com.ep.mqtt.server.vo.TopicVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author zbz
 * @date 2023/11/9 14:59
 */
@Slf4j
@ConditionalOnProperty(prefix = "mqtt.server", value = "mode", havingValue = "aliyun")
@Component
public class AliyunDeal extends Deal {

    @Override
    public void sendMessage(MessageVo messageVo) {
        super.sendMessage(messageVo);
    }

    @Override
    public List<Integer> subscribe(String clientId, List<TopicVo> topicVoList) {
        return super.subscribe(clientId, topicVoList);
    }


}
