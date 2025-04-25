package com.ep.mqtt.server.job;

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
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

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

    private static final Long ING_JOB_SIZE = 30000L;

    private static final LongAdder ING_JOB_COUNTER = new LongAdder();

    @Resource
    private AsyncJobDao asyncJobDao;

    @Resource
    private TransactionUtil transactionUtil;

    @Resource
    private AsyncJobManage asyncJobManage;

    private final Map<AsyncJobBusinessType, AbstractJobProcessor<?>> processorMap;

    public AsyncJobEngine(List<AbstractJobProcessor<?>> abstractJobProcessorList){
        processorMap = abstractJobProcessorList.stream().collect(Collectors.toMap(AbstractJobProcessor::getBusinessType, b -> b));
    }

    public void start(){
        long start = System.currentTimeMillis();
        log.info("start async job engine");

        QUERY_THREAD_POOL.scheduleWithFixedDelay(new QueryJobRunnable(), 60, 500, TimeUnit.MILLISECONDS);

        QUERY_THREAD_POOL.scheduleWithFixedDelay(new QueryTimeoutJobRunnable(), 1, 1, TimeUnit.DAYS);

        QUERY_THREAD_POOL.scheduleWithFixedDelay(new CleanJobRunnable(), 1, 1, TimeUnit.DAYS);

        log.info("complete start async job engine, cost [{}ms]", System.currentTimeMillis() - start);
    }

    public void stop(){
        long start = System.currentTimeMillis();
        log.info("stop async job engine");

        QUERY_THREAD_POOL.shutdown();

        for (AbstractJobProcessor<?> abstractJobProcessor : processorMap.values()){
            if (abstractJobProcessor.getThreadPool().isShutdown()){
                continue;
            }

            abstractJobProcessor.getThreadPool().shutdown();
        }

        log.info("complete stop async job engine, cost [{}ms]", System.currentTimeMillis() - start);
    }

    public class QueryJobRunnable implements Runnable {
        @Override
        public void run() {
            String id = UUID.randomUUID().toString();
            try {
                long jobStart = System.currentTimeMillis();

                long ingJobCountNum = ING_JOB_COUNTER.sum();
                if (ingJobCountNum > ING_JOB_SIZE){
                    return;
                }

                List<AsyncJobDto> pendingJobList = asyncJobDao.getPendingJob(2000);
                if (CollectionUtils.isEmpty(pendingJobList)) {
                    return;
                }

                for (AsyncJobDto pendingJob : pendingJobList) {
                    ING_JOB_COUNTER.increment();

                    occupyJob(pendingJob);
                }

                log.info("获取待执行任务，任务id:{}, 任务数:{}, 正在处理的任务数:{}, 耗时{}ms", id, pendingJobList.size(), ingJobCountNum, System.currentTimeMillis() - jobStart);
            } catch (Throwable e) {
                log.error("获取待执行任务出错，任务id:{}", id, e);
            }
        }
    }

    private void occupyJob(AsyncJobDto pendingJob){
        long start = System.currentTimeMillis();
        boolean isDecrement = true;

        try {
            Boolean isOccupy = transactionUtil.transaction(() -> asyncJobDao.tryOccupyJob(pendingJob.getBusinessId()));
            if (!isOccupy) {
                return;
            }
            AbstractJobProcessor<Object> jobProcessor = (AbstractJobProcessor<Object>)processorMap.get(pendingJob.getBusinessType());

            if (jobProcessor == null) {
                asyncJobDao.finishJob(pendingJob.getBusinessId(), AsyncJobStatus.FINISH, pendingJob.getExecuteNum() + 1,
                        AsyncJobExecuteResult.FAIL, "未找到对应的processor", null);
                return;
            }

            jobProcessor.getThreadPool().submit(() -> {
                executeJob(pendingJob, jobProcessor);

                ING_JOB_COUNTER.decrement();
            });

            isDecrement = false;
            log.info("预占任务：事件类型[{}]，事件id[{}]，耗时[{}ms]", pendingJob.getBusinessType(), pendingJob.getBusinessId(),
                    System.currentTimeMillis() - start);
        }
        catch (Throwable e){
            log.error("预占任务出错：事件类型[{}]，事件id[{}]，耗时[{}ms]", pendingJob.getBusinessType(), pendingJob.getBusinessId(),
                    System.currentTimeMillis() - start, e);
        }
        finally {
            if (isDecrement){
                ING_JOB_COUNTER.decrement();
            }
        }
    }

    private void executeJob(AsyncJobDto pendingJob, AbstractJobProcessor<Object> jobProcessor){
        long start = System.currentTimeMillis();

        try {
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
                        expectExecuteTime = System.currentTimeMillis() + jobProcessor.getRetryInterval() * 1000L;
                    }

                    asyncJobDao.finishJob(asyncJobDto.getBusinessId(), jobStatus, asyncJobDto.getExecuteNum() + 1, executeResult,
                            executeResultDesc, expectExecuteTime);
                }
                else {
                    if (jobProcessor.isRetain()){
                        asyncJobDao.finishJob(asyncJobDto.getBusinessId(), jobStatus, asyncJobDto.getExecuteNum() + 1, executeResult,
                                executeResultDesc, expectExecuteTime);
                    }
                    else {
                        asyncJobDao.deleteById(asyncJobDto.getId());
                    }

                }

                return null;
            });

            log.info("执行任务：事件类型[{}]，事件id[{}]，耗时[{}ms]", pendingJob.getBusinessType(), pendingJob.getBusinessId(),
                    System.currentTimeMillis() - start);
        }
        catch (Throwable e){
            log.error("执行任务：事件类型[{}]，事件id[{}]，耗时[{}ms]", pendingJob.getBusinessType(), pendingJob.getBusinessId(),
                    System.currentTimeMillis() - start, e);
        }
    }

    public class QueryTimeoutJobRunnable implements Runnable {

        @Override
        public void run() {
            Date executeTime = new Date();
            DateUtils.setHours(executeTime, 23);
            DateUtils.setMinutes(executeTime, 0);
            DateUtils.setSeconds(executeTime, 0);
            DateUtils.setMilliseconds(executeTime, 0);

            AsyncJobDto queryTimeoutJob = asyncJobDao.lock(AsyncJobBusinessType.QUERY_TIMEOUT_JOB.getBusinessId());
            if (queryTimeoutJob == null) {
                asyncJobManage.addJob(AsyncJobBusinessType.QUERY_TIMEOUT_JOB.getBusinessId(), AsyncJobBusinessType.DISPATCH_MESSAGE, null,
                    executeTime);
                return;
            }

            if (AsyncJobStatus.EXECUTING.equals(queryTimeoutJob.getExecuteStatus())
                || AsyncJobStatus.READY.equals(queryTimeoutJob.getExecuteStatus())) {
                return;
            }

            asyncJobDao.deleteById(queryTimeoutJob.getId());
            asyncJobManage.addJob(AsyncJobBusinessType.QUERY_TIMEOUT_JOB.getBusinessId(), AsyncJobBusinessType.DISPATCH_MESSAGE, null, executeTime);
        }
    }

    public class CleanJobRunnable implements Runnable {

        @Override
        public void run() {
            Date executeTime = new Date();
            DateUtils.setHours(executeTime, 23);
            DateUtils.setMinutes(executeTime, 0);
            DateUtils.setSeconds(executeTime, 0);
            DateUtils.setMilliseconds(executeTime, 0);

            AsyncJobDto clearJob = asyncJobDao.lock(AsyncJobBusinessType.CLEAR_JOB.getBusinessId());
            if (clearJob == null) {
                asyncJobManage.addJob(AsyncJobBusinessType.CLEAR_JOB.getBusinessId(), AsyncJobBusinessType.CLEAR_JOB, null, executeTime);
                return;
            }

            if (AsyncJobStatus.EXECUTING.equals(clearJob.getExecuteStatus()) || AsyncJobStatus.READY.equals(clearJob.getExecuteStatus())) {
                return;
            }

            asyncJobDao.deleteById(clearJob.getId());
            asyncJobManage.addJob(AsyncJobBusinessType.CLEAR_JOB.getBusinessId(), AsyncJobBusinessType.CLEAR_JOB, null, executeTime);
        }
    }
}
