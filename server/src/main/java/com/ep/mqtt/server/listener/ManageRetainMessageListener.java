package com.ep.mqtt.server.listener;

import com.ep.mqtt.server.listener.msg.ManageRetainMessageMsg;
import com.ep.mqtt.server.metadata.ChannelKey;
import com.ep.mqtt.server.metadata.LocalLockKey;
import com.ep.mqtt.server.store.RetainMessageStore;
import com.ep.mqtt.server.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author : zbz
 * @date : 2023/9/3
 */
@Component
public class ManageRetainMessageListener extends AbstractListener<ManageRetainMessageMsg> {

    @Autowired
    private RetainMessageStore retainMessageStore;

    @Override
    public void deal(ManageRetainMessageMsg message) {
        synchronized (LocalLockKey.LOCK_MANAGE_RETAIN_MESSAGE.formatKey(message.getMessageVo().getTopic()).intern()){
            if (ManageRetainMessageMsg.ManageType.ADD.getKey().equals(message.getManageType())) {
                retainMessageStore.addRetainMessage(message.getMessageVo().getTopic(), message.getMessageVo());
            } else if (ManageRetainMessageMsg.ManageType.REMOVE.getKey().equals(message.getManageType())) {
                retainMessageStore.removeRetainMessage(message.getMessageVo().getTopic());
            }
        }
    }

    @Override
    public ChannelKey getChannelKey() {
        return ChannelKey.MANAGE_RETAIN_MESSAGE;
    }

    @Override
    public ManageRetainMessageMsg convertMessage(String messageStr) {
        return JsonUtil.string2Obj(messageStr, ManageRetainMessageMsg.class);
    }
}
