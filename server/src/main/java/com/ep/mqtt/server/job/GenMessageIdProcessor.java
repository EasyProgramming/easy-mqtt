package com.ep.mqtt.server.job;

import com.ep.mqtt.server.db.dao.MessageIdProgressDao;
import com.ep.mqtt.server.db.dao.SendMessageDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.*;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.ep.mqtt.server.raft.transfer.SendMessage;
import com.ep.mqtt.server.raft.transfer.TransferData;
import com.ep.mqtt.server.util.JsonUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zbz
 * @date 2025/2/27 11:31
 */
@Slf4j
@Component
public class GenMessageIdProcessor extends AbstractJobProcessor<GenMessageIdParam> {

    @Resource
    private MessageIdProgressDao messageIdProgressDao;

    @Resource
    private SendMessageDao sendMessageDao;

    public GenMessageIdProcessor() {
        super(new ThreadPoolExecutor(Constant.PROCESSOR_NUM * 8, Constant.PROCESSOR_NUM * 8, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder().setNameFormat("gen-message-id-%s").build()));
    }

    @Override
    public AsyncJobExecuteResult process(AsyncJobDto asyncJobDto, GenMessageIdParam jobParam) {
        StopWatch stopWatch = new StopWatch("生成消息id");

        stopWatch.start("id自增");
        Integer messageId = messageIdProgressDao.genMessageId(jobParam.getToClientId());
        stopWatch.stop();

        if (messageId == null){
            return AsyncJobExecuteResult.SUCCESS;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setSendQos(jobParam.getSendQos());
        sendMessage.setTopic(jobParam.getTopic());
        sendMessage.setSendPacketId(messageId);
        sendMessage.setToClientId(jobParam.getToClientId());
        sendMessage.setPayload(jobParam.getPayload());
        sendMessage.setIsDup(false);
        sendMessage.setIsRetain(jobParam.getIsRetain().getBoolean());

        if (jobParam.getSendQos() == Qos.LEVEL_0) {
            stopWatch.start("发送raft消息-qos0");
            EasyMqttRaftClient.asyncSendReadOnly(JsonUtil.obj2String(
                    new TransferData(RaftCommand.SEND_MESSAGE, JsonUtil.obj2String(sendMessage))));
            stopWatch.stop();
        }
        else {
            if (sendMessageDao.updateSendPacketId(jobParam.getSendMessageId(), messageId)) {
                stopWatch.start("发送raft消息-qos12");
                EasyMqttRaftClient.asyncSendReadOnly(JsonUtil.obj2String(new TransferData(RaftCommand.SEND_MESSAGE, JsonUtil.obj2String(sendMessage))));
                stopWatch.stop();
            }
        }

        log.info(stopWatch.prettyPrint());
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
