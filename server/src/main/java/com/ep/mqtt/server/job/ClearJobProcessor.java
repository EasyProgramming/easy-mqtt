package com.ep.mqtt.server.job;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ep.mqtt.server.db.dao.AsyncJobDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobExecuteResult;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.util.DateUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @author : zbz
 * @date : 2025/3/2
 */
@Slf4j
@Component
public class ClearJobProcessor extends AbstractJobProcessor<Void> {

    @Resource
    private AsyncJobDao asyncJobDao;

    public ClearJobProcessor() {
        super(new ThreadPoolExecutor(Constant.PROCESSOR_NUM * 2, Constant.PROCESSOR_NUM * 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("clear-job-%s").build()));
    }

    @Override
    public AsyncJobExecuteResult process(AsyncJobDto asyncJobDto, Void jobParam) {
        Date now = DateUtil.getMidnight();
        String id = UUID.randomUUID().toString();
        long jobStart = System.currentTimeMillis();
        log.info("开始清理成功任务，任务id:{}", id);

        int size = 1000;
        while (true) {
            List<AsyncJobDto> successJobList = asyncJobDao.getExecuteSuccessJob(size, now);
            if (CollectionUtils.isEmpty(successJobList)) {
                break;
            }

            asyncJobDao.deleteByIds(successJobList.stream().map(AsyncJobDto::getId).collect(Collectors.toSet()));

            if (successJobList.size() < size) {
                break;
            }
        }

        log.info("结束清理成功任务，任务id:{}, 耗时{}ms", id, System.currentTimeMillis() - jobStart);
        return AsyncJobExecuteResult.SUCCESS;
    }

    @NonNull
    @Override
    public AsyncJobBusinessType getBusinessType() {
        return AsyncJobBusinessType.CLEAR_JOB;
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
