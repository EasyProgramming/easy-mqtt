package com.ep.mqtt.server.deal;

import com.ep.mqtt.server.db.dao.*;
import com.ep.mqtt.server.job.AsyncJobManage;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.DisconnectReason;
import com.ep.mqtt.server.metadata.YesOrNo;
import com.ep.mqtt.server.session.Session;
import com.ep.mqtt.server.util.ModelUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

/**
 * @author zbz
 * @date 2025/3/8 14:00
 */
@Component
public class CommonDeal {

    @Resource
    private ClientDao clientDao;

    @Resource
    private ClientSubscribeDao clientSubscribeDao;

    @Resource
    private ReceiveQos2MessageDao receiveQos2MessageDao;

    @Resource
    private SendMessageDao sendMessageDao;

    @Resource
    private MessageIdProgressDao messageIdProgressDao;

    @Resource
    private AsyncJobManage asyncJobManage;

    @Transactional(rollbackFor = Exception.class)
    public void clearClientData(String clientId) {
        clientDao.deleteByClientId(clientId);
        clientSubscribeDao.deleteByClientId(clientId);
        receiveQos2MessageDao.deleteByFromClientId(clientId);
        sendMessageDao.deleteByToClientId(clientId);
        messageIdProgressDao.deleteByClientId(clientId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void afterDisconnect(DisconnectReason disconnectReason, Session session){
        if (!DisconnectReason.REPEAT_CONNECT.equals(disconnectReason)){
            if (session!= null && session.getIsCleanSession()){
                clearClientData(session.getClientId());
            }
        }

        if (!DisconnectReason.NORMAL.equals(disconnectReason) && session!= null && session.getWillMessage() != null){
            asyncJobManage.addJob(
                    AsyncJobBusinessType.DISPATCH_MESSAGE.getBusinessId(UUID.randomUUID().toString()),
                    AsyncJobBusinessType.DISPATCH_MESSAGE,
                    ModelUtil.buildDispatchMessageParam(
                            session.getWillMessage().getQos(),
                            session.getWillMessage().getTopic(),
                            -1,
                            session.getClientId(),
                            session.getWillMessage().getPayload(),
                            session.getWillMessage().getIsRetain() ? YesOrNo.YES : YesOrNo.NO
                    ),
                    new Date());
        }
    }

}
