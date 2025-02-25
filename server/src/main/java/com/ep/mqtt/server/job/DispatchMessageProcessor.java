package com.ep.mqtt.server.job;

import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobExecuteResult;
import com.ep.mqtt.server.metadata.Constant;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author : zbz
 * @date : 2025/2/1
 */
@Component
public class DispatchMessageProcessor extends AbstractJobProcessor<DispatchMessageParam> {

    public DispatchMessageProcessor() {
        super(new ThreadPoolExecutor(Constant.PROCESSOR_NUM * 2, Constant.PROCESSOR_NUM * 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("dispatch-message-%s").build()));
    }

    @Override
    public AsyncJobExecuteResult process(AsyncJobDto asyncJobDto, DispatchMessageParam jobParam) {
        // 匹配topic filter

        // 根据topic filter及id游标查询匹配的客户端，并计算qos

        // 批量插入发送消息表

        // 批量插入生成消息id的异步任务表

        return null;
    }

    @NonNull
    @Override
    public AsyncJobBusinessType getBusinessType() {
        return AsyncJobBusinessType.DISPATCH_MESSAGE;
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
