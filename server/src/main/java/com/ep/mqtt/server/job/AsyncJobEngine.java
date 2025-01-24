package com.ep.mqtt.server.job;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.ep.mqtt.server.db.dao.AsyncJobDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobExecuteResult;
import com.ep.mqtt.server.metadata.AsyncJobStatus;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.util.JsonUtil;
import com.ep.mqtt.server.util.TransactionUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2024/1/4 15:43
 */
@Slf4j
@Component
public class AsyncJobEngine {

    /**
     * 查询任务的线程池
     */
    private static final ScheduledThreadPoolExecutor QUERY_THREAD_POOL =
        new ScheduledThreadPoolExecutor(Constant.PROCESSOR_NUM, new ThreadFactoryBuilder().setNameFormat("async-job-query-%s").build());

    @Resource
    private AsyncJobDao asyncJobDao;

    @Resource
    private TransactionUtil transactionUtil;

    private Map<AsyncJobBusinessType, AbstractJobProcessor<Object>> processorMap;

    @PostConstruct
    public void init(List<AbstractJobProcessor<Object>> abstractJobProcessorList) {
        QUERY_THREAD_POOL.scheduleWithFixedDelay(new QueryJobRunnable(), 60, 1, TimeUnit.SECONDS);

        QUERY_THREAD_POOL.scheduleWithFixedDelay(new QueryTimeoutJobRunnable(), 1, 5, TimeUnit.MINUTES);

        processorMap = abstractJobProcessorList.stream().collect(Collectors.toMap(AbstractJobProcessor::getBusinessType, b -> b));
    }

    public class QueryJobRunnable implements Runnable {
        @Override
        public void run() {
            String id = UUID.randomUUID().toString();
            try {
                long jobStart = System.currentTimeMillis();

                List<AsyncJobDto> pendingJobList = asyncJobDao.getPendingJob(1000);
                if (CollectionUtils.isEmpty(pendingJobList)) {
                    return;
                }

                for (AsyncJobDto pendingJob : pendingJobList) {
                    Boolean isOccupy = transactionUtil.transaction(() -> asyncJobDao.tryOccupyJob(pendingJob.getBusinessId()));
                    if (!isOccupy) {
                        continue;
                    }

                    AbstractJobProcessor<Object> jobProcessor = processorMap.get(pendingJob.getBusinessType());
                    if (jobProcessor == null) {
                        asyncJobDao.finishJob(pendingJob.getBusinessId(), AsyncJobStatus.FINISH, pendingJob.getExecuteNum() + 1,
                            AsyncJobExecuteResult.FAIL, "未找到对应的processor", null);
                        continue;
                    }

                    jobProcessor.getThreadPool().submit(() -> {
                        long start = System.currentTimeMillis();

                        transactionUtil.transaction(() -> {
                            AsyncJobDto asyncJobDto;
                            try {
                                asyncJobDto = asyncJobDao.lock(pendingJob.getBusinessId());
                                if (asyncJobDto == null) {
                                    return null;
                                }
                                if (!AsyncJobStatus.EXECUTING.equals(asyncJobDto.getExecuteStatus())) {
                                    return null;
                                }
                            } catch (Throwable e) {
                                log.error("执行异步任务时出现异常:[获取任务失败]", e);
                                asyncJobDao.finishJob(pendingJob.getBusinessId(), AsyncJobStatus.READY, pendingJob.getExecuteNum(),
                                    AsyncJobExecuteResult.FAIL, e.getMessage(),
                                    pendingJob.getExpectExecuteTime() + jobProcessor.getRetryInterval() * 1000L);
                                return null;
                            }

                            AsyncJobExecuteResult executeResult;
                            String executeResultDesc;
                            try {
                                executeResult = jobProcessor.process(asyncJobDto, JsonUtil.string2Obj(asyncJobDto.getJobParam(),
                                    ReflectionKit.getSuperClassGenericType(jobProcessor.getClass(), AbstractJobProcessor.class, 0)));
                                // null代表成功执行任务
                                if (executeResult == null) {
                                    executeResult = AsyncJobExecuteResult.SUCCESS;
                                }

                                executeResultDesc = "成功";
                            } catch (Throwable e) {
                                log.error("执行异步任务时出现异常:[执行业务逻辑失败]", e);
                                executeResult = AsyncJobExecuteResult.FAIL;
                                executeResultDesc = e.getMessage();
                            }

                            AsyncJobStatus jobStatus = AsyncJobStatus.FINISH;
                            Long expectExecuteTime = null;
                            if (!AsyncJobExecuteResult.SUCCESS.equals(executeResult)) {
                                if (jobProcessor.getMaxRetryNum() == null || jobProcessor.getMaxRetryNum() > asyncJobDto.getExecuteNum()) {
                                    jobStatus = AsyncJobStatus.READY;
                                    expectExecuteTime = asyncJobDto.getExpectExecuteTime() + jobProcessor.getRetryInterval() * 1000L;
                                }
                            }

                            asyncJobDao.finishJob(asyncJobDto.getBusinessId(), jobStatus, asyncJobDto.getExecuteNum() + 1, executeResult,
                                executeResultDesc, expectExecuteTime);
                            return null;
                        });

                        log.info("处理事件：事件类型[{}]，事件id[{}]，耗时[{}ms]", pendingJob.getBusinessType(), pendingJob.getBusinessId(),
                            System.currentTimeMillis() - start);
                    });
                }

                log.info("获取待执行任务，任务id:{}, 耗时{}ms", id, System.currentTimeMillis() - jobStart);
            } catch (Throwable e) {
                log.error("获取待执行任务出错，任务id:{}", id, e);
            }
        }
    }

    public class QueryTimeoutJobRunnable implements Runnable {

        @Override
        public void run() {
            String id = UUID.randomUUID().toString();
            long jobStart = System.currentTimeMillis();
            log.info("开始处理超时任务，任务id:{}", id);

            List<AsyncJobDto> timeoutJobList = asyncJobDao.getExecuteTimeoutJob(1000);
            if (CollectionUtils.isEmpty(timeoutJobList)) {
                return;
            }

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

            log.info("结束处理超时任务，任务id:{}, 耗时{}ms", id, System.currentTimeMillis() - jobStart);
        }
    }

}
