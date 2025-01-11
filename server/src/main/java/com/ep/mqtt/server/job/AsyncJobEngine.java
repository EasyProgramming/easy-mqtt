package com.ep.mqtt.server.job;

import com.ep.mqtt.server.db.dao.AsyncJobDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.util.TransactionUtil;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * 执行任务的线程池
     */
    private static final ThreadPoolExecutor EXECUTE_THREAD_POOL = new ThreadPoolExecutor(
            Constant.PROCESSOR_NUM * 2, Constant.PROCESSOR_NUM * 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("async-job-execute-%s").build());

    @Resource
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Resource
    private TransactionDefinition transactionDefinition;

    @Resource
    private AsyncJobDao asyncJobDao;

    @Resource
    private TransactionUtil transactionUtil;

    @PostConstruct
    public void init(){
        QUERY_THREAD_POOL
                .scheduleWithFixedDelay(new QueryJobRunnable(), 60, 1, TimeUnit.SECONDS);

        QUERY_THREAD_POOL
                .scheduleWithFixedDelay(new QueryTimeoutJobRunnable(), 1, 5, TimeUnit.MINUTES);
    }

    public class QueryJobRunnable implements Runnable {
        @Override
        public void run() {
            String id = UUID.randomUUID().toString();
            try {
                long jobStart = System.currentTimeMillis();

                List<AsyncJobDto> pendingJobList = asyncJobDao.getPendingJob(1000);
                if (CollectionUtils.isEmpty(pendingJobList)){
                    return;
                }

                for (AsyncJobDto pendingJob : pendingJobList){
                    Boolean isOccupy = transactionUtil.transaction(() -> asyncJobDao.tryOccupyJob(pendingJob.getBusinessId()));
                    if (!isOccupy){
                        continue;
                    }

                    EXECUTE_THREAD_POOL.submit(()->{
                        long start = System.currentTimeMillis();

                        transactionUtil.transaction(()->{
                            try {
                                // TODO: 2025/1/9 对数据行进行加锁，然后获取数据行，根据状态做判断，不是执行中的，忽略；数据行不存在的直接结束

                                // TODO: 2025/1/8 找到具体的执行处理器，然后执行
                                convertDataRecordService.deal(convertDataRecordId);

                                // TODO: 2025/1/8 更新任务状态
                                convertDataRecordService.finishRecord(convertDataRecordId, ConvertRecordStatus.SUCCESS.getCode(),
                                        "success", traceInfo, new Date(start));
                            }
                            catch (Throwable e){
                                log.error("deal convert record error", e);

                                // TODO: 2025/1/8 更新任务状态
                                convertDataRecordService.finishRecord(convertDataRecordId, ConvertRecordStatus.FAIL.getCode(),
                                        StringUtils.substring(e.getMessage(), 0, 2000), traceInfo, new Date(start));
                            }
                        });

                        log.info("处理事件：事件类型[{}]，事件id[{}]，耗时[{}ms]", convertDataRecordEntity.getBusinessType(),
                                convertDataRecordEntity.getBusinessId(), System.currentTimeMillis() - start);
                    });
                }
                log.info("完成调度迁移任务，任务id:{}, 耗时{}ms", id, System.currentTimeMillis() - jobStart);
            }
            catch (Throwable e){
                log.error("调度迁移任务出错，任务id:{}", id, e);
            }
        }
    }

    public class QueryTimeoutJobRunnable implements Runnable {

        @Override
        public void run() {
            String id = UUID.randomUUID().toString();
            long jobStart = System.currentTimeMillis();
            log.info("开始处理超时任务，任务id:{}", id);

            Date timeoutDate = DateUtils.addMinutes(new Date(), -30);
            List<ConvertDataRecordEntity> timeoutRecordList = convertDataRecordService.lambdaQuery()
                    .le(ConvertDataRecordEntity::getStartTime, timeoutDate)
                    .eq(ConvertDataRecordEntity::getStatus, ConvertRecordStatus.EXECUTING.getCode())
                    .list();
            if (CollectionUtils.isEmpty(timeoutRecordList)){
                return;
            }
            for (ConvertDataRecordEntity convertDataRecordEntity : timeoutRecordList){
                TransactionUtil.transaction(dataSourceTransactionManager, transactionDefinition, ()->{
                    ConvertDataRecordEntity lastConvertDataRecordEntity = convertDataRecordService.lock(convertDataRecordEntity.getId());
                    if (!ConvertRecordStatus.EXECUTING.getCode().equals(lastConvertDataRecordEntity.getStatus())){
                        return;
                    }
                    convertDataRecordService.lambdaUpdate()
                            .set(ConvertDataRecordEntity::getFinishTime, new Date())
                            .set(ConvertDataRecordEntity::getStatus, ConvertRecordStatus.FAIL.getCode())
                            .set(ConvertDataRecordEntity::getResultDesc, "timeout")
                            .eq(ConvertDataRecordEntity::getId, lastConvertDataRecordEntity.getId())
                            .update();
                });
            }

            log.info("结束处理超时任务，任务id:{}, 耗时{}ms", id, System.currentTimeMillis() - jobStart);
        }
    }

    public static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String myNamePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = myNamePrefix +
                    POOL_NUMBER.getAndIncrement() +
                    "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

}
