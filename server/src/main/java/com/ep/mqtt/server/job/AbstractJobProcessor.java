package com.ep.mqtt.server.job;

import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobExecuteResult;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author : zbz
 * @date : 2025/1/12
 */
public abstract class AbstractJobProcessor<K extends Object> {

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
     * @param jobParam
     *            任务参数
     * @return 执行结果
     */
    @Nullable
    public abstract AsyncJobExecuteResult process(AsyncJobDto asyncJobDto, K jobParam);

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

    /**
     * 是否保留成功执行的任务
     * @return 开关
     */
    public boolean isRetain(){
        return true;
    }

}
