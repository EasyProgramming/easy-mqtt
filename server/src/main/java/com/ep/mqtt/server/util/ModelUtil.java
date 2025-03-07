package com.ep.mqtt.server.util;

import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.job.DispatchMessageParam;
import com.ep.mqtt.server.job.GenMessageIdParam;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobStatus;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;

import java.util.UUID;

/**
 * @author zbz
 * @date 2025/3/7 15:24
 */
public class ModelUtil {

    public static DispatchMessageParam buildDispatchMessageParam(Qos receiveQos, String topic, Integer receivePacketId, String fromClientId,
                                                                 String payload){
        DispatchMessageParam dispatchMessageParam = new DispatchMessageParam();
        dispatchMessageParam.setReceiveQos(receiveQos);
        dispatchMessageParam.setTopic(topic);
        dispatchMessageParam.setReceivePacketId(receivePacketId);
        dispatchMessageParam.setFromClientId(fromClientId);
        dispatchMessageParam.setPayload(payload);

        return dispatchMessageParam;
    }

    public static GenMessageIdParam buildGenMessageIdParam(Long sendMessageId, Qos sendQos, String topic, String toClientId, String payload, YesOrNo isRetain){
        GenMessageIdParam genMessageIdParam = new GenMessageIdParam();
        genMessageIdParam.setSendMessageId(sendMessageId);
        genMessageIdParam.setSendQos(sendQos);
        genMessageIdParam.setTopic(topic);
        genMessageIdParam.setToClientId(toClientId);
        genMessageIdParam.setPayload(payload);
        genMessageIdParam.setIsRetain(isRetain);

        return genMessageIdParam;
    }

    public static <T> AsyncJobDto buildAsyncJobDto(String businessId, AsyncJobBusinessType asyncJobBusinessType, Long expectExecuteTime,
                                                   Integer executeNum, AsyncJobStatus executeStatus, T jobParam){
        AsyncJobDto asyncJobDto = new AsyncJobDto();
        asyncJobDto.setBusinessId(businessId);
        asyncJobDto.setBusinessType(asyncJobBusinessType);
        asyncJobDto.setExpectExecuteTime(expectExecuteTime);
        asyncJobDto.setExecuteNum(executeNum);
        asyncJobDto.setExecuteStatus(executeStatus);
        asyncJobDto.setJobParam(JsonUtil.obj2String(jobParam));

        return asyncJobDto;
    }

    public static SendMessageDto buildSendMessageDto(Qos receiveQos, Integer receivePacketId, String fromClientId, Qos sendQos, String topic,
                                                     Integer sendPacketId, String toClientId, String payload, YesOrNo isReceivePubRec,
                                                     Long validTime, YesOrNo isRetain){
        SendMessageDto sendMessageDto = new SendMessageDto();
        sendMessageDto.setReceiveQos(receiveQos);
        sendMessageDto.setReceivePacketId(receivePacketId);
        sendMessageDto.setFromClientId(fromClientId);
        sendMessageDto.setSendQos(sendQos);
        sendMessageDto.setTopic(topic);
        sendMessageDto.setSendPacketId(sendPacketId);
        sendMessageDto.setToClientId(toClientId);
        sendMessageDto.setPayload(payload);
        sendMessageDto.setIsReceivePubRec(isReceivePubRec);
        sendMessageDto.setValidTime(validTime);
        sendMessageDto.setIsRetain(isRetain);
        return sendMessageDto;
    }

    public static AsyncJobDto buildGenMessageIdAsyncJobDto(SendMessageDto sendMessageDto, Long now) {
        GenMessageIdParam genMessageIdParam = ModelUtil.buildGenMessageIdParam(
                sendMessageDto.getId(),
                sendMessageDto.getSendQos(),
                sendMessageDto.getTopic(),
                sendMessageDto.getToClientId(),
                sendMessageDto.getPayload(),
                sendMessageDto.getIsRetain()
        );

        String businessId;
        if (sendMessageDto.getSendQos() == Qos.LEVEL_0) {
            businessId = AsyncJobBusinessType.GEN_MESSAGE_ID.getBusinessId(UUID.randomUUID().toString());
        } else {
            businessId = AsyncJobBusinessType.GEN_MESSAGE_ID.getBusinessId(sendMessageDto.getId());
        }

        return ModelUtil.buildAsyncJobDto(businessId, AsyncJobBusinessType.GEN_MESSAGE_ID, now, 0, AsyncJobStatus.READY, genMessageIdParam);
    }
}
