package com.ep.mqtt.server.job;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.ep.mqtt.server.db.dao.MessageIdProgressDao;
import com.ep.mqtt.server.db.dao.SendMessageDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.*;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.ep.mqtt.server.raft.transfer.SendMessage;
import com.ep.mqtt.server.raft.transfer.TransferData;
import com.ep.mqtt.server.util.JsonUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author zbz
 * @date 2025/2/27 11:31
 */
@Component
public class GenMessageIdProcessor extends AbstractJobProcessor<GenMessageIdParam> {

    @Resource
    private MessageIdProgressDao messageIdProgressDao;

    @Resource
    private SendMessageDao sendMessageDao;

    public GenMessageIdProcessor() {
        super(new ThreadPoolExecutor(Constant.PROCESSOR_NUM * 2, Constant.PROCESSOR_NUM * 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder().setNameFormat("gen-message-id-%s").build()));
    }

    @Override
    public AsyncJobExecuteResult process(AsyncJobDto asyncJobDto, GenMessageIdParam jobParam) {
        Long messageId = messageIdProgressDao.genMessageId(jobParam.getToClientId());
        if (messageId == null){
            return AsyncJobExecuteResult.SUCCESS;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setSendQos(jobParam.getSendQos());
        sendMessage.setTopic(jobParam.getTopic());
        sendMessage.setSendPacketId(String.valueOf(messageId));
        sendMessage.setToClientId(jobParam.getToClientId());
        sendMessage.setPayload(jobParam.getPayload());
        sendMessage.setIsDup(false);
        sendMessage.setIsRetain(jobParam.getIsRetain().getBoolean());

        if (jobParam.getSendQos() == Qos.LEVEL_0) {
            EasyMqttRaftClient.syncSend(JsonUtil.obj2String(
                    new TransferData(RaftCommand.SEND_MESSAGE, JsonUtil.obj2String(sendMessage))));
        }
        else {
            if (sendMessageDao.updateSendPacketId(jobParam.getSendMessageId(), String.valueOf(messageId))) {
                EasyMqttRaftClient.syncSend(JsonUtil.obj2String(new TransferData(RaftCommand.SEND_MESSAGE, JsonUtil.obj2String(sendMessage))));
            }
        }

        return AsyncJobExecuteResult.SUCCESS;
    }

    @NonNull
    @Override
    public AsyncJobBusinessType getBusinessType() {
        return AsyncJobBusinessType.GEN_MESSAGE_ID;
    }

    @NonNull
    @Override
    public Integer getRetryInterval() {
        return 60;
    }

    @Override
    public Integer getMaxRetryNum() {
        return null;
    }
}
