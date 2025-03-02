package com.ep.mqtt.server.job;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ep.mqtt.server.db.dao.AsyncJobDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobExecuteResult;
import com.ep.mqtt.server.metadata.AsyncJobStatus;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.util.DateUtil;
import com.ep.mqtt.server.util.TransactionUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @author : zbz
 * @date : 2025/3/2
 */
@Slf4j
@Component
public class QueryTimeoutJobProcessor extends AbstractJobProcessor<Void> {

    @Resource
    private AsyncJobDao asyncJobDao;

    @Resource
    private TransactionUtil transactionUtil;

    public QueryTimeoutJobProcessor() {
        super(new ThreadPoolExecutor(Constant.PROCESSOR_NUM * 2, Constant.PROCESSOR_NUM * 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("query-timeout-job-%s").build()));
    }

    @Override
    public AsyncJobExecuteResult process(AsyncJobDto asyncJobDto, Void jobParam) {
        Date now = DateUtil.getMidnight();
        String id = UUID.randomUUID().toString();
        long jobStart = System.currentTimeMillis();
        log.info("开始处理超时任务，任务id:{}", id);

        int size = 1000;
        List<AsyncJobDto> timeoutJobList;
        do {
            timeoutJobList = asyncJobDao.getExecuteTimeoutJob(size, now);

            for (AsyncJobDto timeoutJob : timeoutJobList) {
                transactionUtil.transaction(() -> {
                    AsyncJobDto lastAsyncJobDto = asyncJobDao.lock(timeoutJob.getBusinessId());

                    if (!AsyncJobStatus.EXECUTING.equals(lastAsyncJobDto.getExecuteStatus())) {
                        return null;
                    }

                    asyncJobDao.finishJob(lastAsyncJobDto.getBusinessId(), AsyncJobStatus.FINISH, lastAsyncJobDto.getExecuteNum(),
                        AsyncJobExecuteResult.FAIL, "执行超时", null);
                    return null;
                });
            }

        } while (!CollectionUtils.isEmpty(timeoutJobList) && timeoutJobList.size() >= size);

        log.info("结束处理超时任务，任务id:{}, 耗时{}ms", id, System.currentTimeMillis() - jobStart);

        return AsyncJobExecuteResult.SUCCESS;
    }

    @NonNull
    @Override
    public AsyncJobBusinessType getBusinessType() {
        return AsyncJobBusinessType.QUERY_TIMEOUT_JOB;
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
