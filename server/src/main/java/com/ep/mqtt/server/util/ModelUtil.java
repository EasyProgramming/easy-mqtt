package com.ep.mqtt.server.util;

import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.job.DispatchMessageParam;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobStatus;
import com.ep.mqtt.server.metadata.Qos;

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

}
