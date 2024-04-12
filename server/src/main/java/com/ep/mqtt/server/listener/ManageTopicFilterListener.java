package com.ep.mqtt.server.listener;

import com.ep.mqtt.server.listener.msg.ManageTopicFilterMsg;
import com.ep.mqtt.server.metadata.ChannelKey;
import com.ep.mqtt.server.metadata.LocalLockKey;
import com.ep.mqtt.server.store.SubscribeRelStore;
import com.ep.mqtt.server.util.JsonUtil;
import com.ep.mqtt.server.vo.TopicVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 同步订阅信息，如果需要保证订阅后就一定能收到消息，可以改为RPC同步调用
 * 
 * @author : zbz
 * @date : 2023/8/18
 */
@Slf4j
@Component
public class ManageTopicFilterListener extends AbstractListener<ManageTopicFilterMsg> {

    @Autowired
    private SubscribeRelStore subscribeRelStore;

    @Override
    public void deal(ManageTopicFilterMsg manageTopicFilterMsg) {
        if (manageTopicFilterMsg == null) {
            return;
        }
        synchronized (LocalLockKey.LOCK_MANAGE_TOPIC_FILTER.formatKey(manageTopicFilterMsg.getClientId()).intern()){
            if (ManageTopicFilterMsg.ManageType.SUBSCRIBE.getKey().equals(manageTopicFilterMsg.getManageType())) {
                for (TopicVo topicVo : manageTopicFilterMsg.getTopicVoList()) {
                    subscribeRelStore.subscribe(topicVo.getTopicFilter(), manageTopicFilterMsg.getClientId(),
                            topicVo.getQos());
                }
            } else if (ManageTopicFilterMsg.ManageType.UN_SUBSCRIBE.getKey().equals(manageTopicFilterMsg.getManageType())) {
                if (CollectionUtils.isEmpty(manageTopicFilterMsg.getTopicVoList())) {
                    subscribeRelStore.unSubscribe(manageTopicFilterMsg.getClientId());
                } else {
                    for (TopicVo topicVo : manageTopicFilterMsg.getTopicVoList()) {
                        subscribeRelStore.unSubscribe(topicVo.getTopicFilter(), manageTopicFilterMsg.getClientId());
                    }
                }
            }
        }
    }

    @Override
    public ChannelKey getChannelKey() {
        return ChannelKey.MANAGE_TOPIC_FILTER;
    }

    @Override
    public ManageTopicFilterMsg convertMessage(String messageStr) {
        return JsonUtil.string2Obj(messageStr, ManageTopicFilterMsg.class);
    }

}
