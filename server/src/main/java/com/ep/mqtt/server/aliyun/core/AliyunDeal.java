package com.ep.mqtt.server.aliyun.core;

import com.ep.mqtt.server.deal.Deal;
import com.ep.mqtt.server.util.TopicUtil;
import com.ep.mqtt.server.vo.MessageVo;
import com.ep.mqtt.server.vo.TopicVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author zbz
 * @date 2023/11/9 14:59
 */
@Slf4j
public class AliyunDeal extends Deal {

    private static final Integer P2P_PARAMS_SIZE = 3;

    private static final String P2P_FLAG = "/p2p/";

    @Override
    public void sendMessage(MessageVo messageVo) {
        boolean isP2P = false;
        String[] splitResult = StringUtils.split(messageVo.getTopic(), P2P_FLAG);
        if (splitResult.length == P2P_PARAMS_SIZE){
            if (!StringUtils.containsAny(splitResult[0], TopicUtil.TOPIC_SPLIT_FLAG)){
                isP2P = true;
                messageVo.setTopic(splitResult[0]);
                messageVo.setToClientId(splitResult[2]);
            }
        }
        super.sendMessage(messageVo, isP2P);
    }

    @Override
    public List<Integer> subscribe(String clientId, List<TopicVo> topicVoList) {
        // TODO: 2023/11/9 过滤特殊的p2p topic

        return super.subscribe(clientId, topicVoList);
    }


}
