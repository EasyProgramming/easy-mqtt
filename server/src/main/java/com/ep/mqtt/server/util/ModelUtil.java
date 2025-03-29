package com.ep.mqtt.server.util;

import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.job.DispatchMessageParam;
import com.ep.mqtt.server.job.GenMessageIdParam;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobStatus;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;

/**
 * @author zbz
 * @date 2025/3/7 15:24
 */
public class ModelUtil {

    public static DispatchMessageParam buildDispatchMessageParam(Qos receiveQos, String topic, Integer receivePacketId, String fromClientId,
                                                                 String payload, YesOrNo isRetain){
        DispatchMessageParam dispatchMessageParam = new DispatchMessageParam();
        dispatchMessageParam.setReceiveQos(receiveQos);
        dispatchMessageParam.setTopic(topic);
        dispatchMessageParam.setReceivePacketId(receivePacketId);
        dispatchMessageParam.setFromClientId(fromClientId);
        dispatchMessageParam.setPayload(payload);
        dispatchMessageParam.setIsRetain(isRetain);

        return dispatchMessageParam;
    }

    public static GenMessageIdParam buildGenMessageIdParam(Qos receiveQos, Integer receivePacketId, String fromClientId, Qos sendQos, String topic,
                                                           String toClientId, String payload, YesOrNo isReceivePubRec, YesOrNo isRetain){
        GenMessageIdParam genMessageIdParam = new GenMessageIdParam();
        genMessageIdParam.setReceiveQos(receiveQos);
        genMessageIdParam.setReceivePacketId(receivePacketId);
        genMessageIdParam.setFromClientId(fromClientId);
        genMessageIdParam.setSendQos(sendQos);
        genMessageIdParam.setTopic(topic);
        genMessageIdParam.setToClientId(toClientId);
        genMessageIdParam.setPayload(payload);
        genMessageIdParam.setIsReceivePubRec(isReceivePubRec);
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

}
