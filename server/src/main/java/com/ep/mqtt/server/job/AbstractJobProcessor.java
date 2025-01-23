package com.ep.mqtt.server.job;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobExecuteResult;

/**
 * @author : zbz
 * @date : 2025/1/12
 */
public abstract class AbstractJobProcessor {

    /**
     * 用于执行任务的线程池
     */
    private final ThreadPoolExecutor threadPool;

    public AbstractJobProcessor(ThreadPoolExecutor threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * 任务处理逻辑
     * 
     * @param asyncJobDto
     *            任务信息
     * @return 执行结果
     */
    @Nullable
    public abstract AsyncJobExecuteResult process(AsyncJobDto asyncJobDto);

    /**
     * 获取业务类型
     * 
     * @return 业务类型
     */
    @NonNull
    public abstract AsyncJobBusinessType getBusinessType();

    /**
     * 获取线程池（用于执行该任务）
     * 
     * @return 线程池
     */
    @NonNull
    public ThreadPoolExecutor getThreadPool() {
        return this.threadPool;
    }

    /**
     * 获取重试间隔
     * 
     * @return 间隔时间（单位：秒）
     */
    @NonNull
    public abstract Integer getRetryInterval();

    /**
     * 获取最大重试次数（null代表无限制）
     *
     * @return 最大重试次数
     */
    @Nullable
    public abstract Integer getMaxRetryNum();

}
