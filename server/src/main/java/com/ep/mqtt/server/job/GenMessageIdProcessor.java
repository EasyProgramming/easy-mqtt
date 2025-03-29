package com.ep.mqtt.server.job;

import com.ep.mqtt.server.db.dao.ClientDao;
import com.ep.mqtt.server.db.dao.SendMessageDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.db.dto.ClientDto;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.metadata.*;
import com.ep.mqtt.server.rpc.EasyMqttRpcClient;
import com.ep.mqtt.server.rpc.transfer.SendMessage;
import com.ep.mqtt.server.util.ModelUtil;
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
    private SendMessageDao sendMessageDao;

    @Resource
    private ClientDao clientDao;

    public GenMessageIdProcessor() {
        super(new ThreadPoolExecutor(Constant.PROCESSOR_NUM * 8, Constant.PROCESSOR_NUM * 8, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder().setNameFormat("gen-message-id-%s").build()));
    }

    @Override
    public AsyncJobExecuteResult process(AsyncJobDto asyncJobDto, GenMessageIdParam jobParam) {
        StopWatch stopWatch = new StopWatch("生成消息id");

        stopWatch.start("对客户端加锁");
        ClientDto clientDto = clientDao.lock(jobParam.getToClientId());
        stopWatch.stop();
        if (clientDto == null){
            log.info(stopWatch.prettyPrint());
            return AsyncJobExecuteResult.SUCCESS;
        }

        stopWatch.start("id自增");
        long lastMessageIdProgress = clientDto.getMessageIdProgress() + 1L;
        clientDao.updateMessageIdProgress(clientDto.getClientId(), lastMessageIdProgress);
        int messageId = (int) (lastMessageIdProgress % 65535);
        stopWatch.stop();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setSendQos(jobParam.getSendQos());
        sendMessage.setTopic(jobParam.getTopic());
        sendMessage.setSendPacketId(messageId);
        sendMessage.setToClientId(jobParam.getToClientId());
        sendMessage.setPayload(jobParam.getPayload());
        sendMessage.setIsDup(false);
        sendMessage.setIsRetain(jobParam.getIsRetain().getBoolean());

        if (jobParam.getSendQos() != Qos.LEVEL_0) {
            stopWatch.start("插入数据库");
            SendMessageDto sendMessageDto = ModelUtil.buildSendMessageDto(
                    jobParam.getReceiveQos(),
                    jobParam.getReceivePacketId(),
                    jobParam.getFromClientId(),
                    jobParam.getSendQos(),
                    jobParam.getTopic(),
                    messageId,
                    jobParam.getToClientId(),
                    jobParam.getPayload(),
                    jobParam.getIsReceivePubRec(),
                    System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7,
                    jobParam.getIsRetain()
            );
            stopWatch.stop();

            sendMessageDao.insert(sendMessageDto);
        }

        stopWatch.start("发送rpc消息");
        EasyMqttRpcClient.broadcast(RpcCommand.SEND_MESSAGE, sendMessage);
        stopWatch.stop();

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
