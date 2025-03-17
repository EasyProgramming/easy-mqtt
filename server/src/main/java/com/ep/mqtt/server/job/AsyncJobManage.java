package com.ep.mqtt.server.job;

import com.ep.mqtt.server.db.dao.AsyncJobDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobStatus;
import com.ep.mqtt.server.util.ModelUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author : zbz
 * @date : 2025/1/24
 */
@Slf4j
@Service
public class AsyncJobManage {

    @Resource
    private AsyncJobDao asyncJobDao;

    /**
     * 新增异步任务
     * 
     * @param businessId
     *            业务id
     * @param businessType
     *            业务类型
     * @param jobParam
     *            任务参数
     * @param expectExecuteTime
     *            预计执行时间
     * @param <T>
     *            任务参数泛型
     */
    public <T> void addJob(String businessId, AsyncJobBusinessType businessType, T jobParam, Date expectExecuteTime) {
        try {
            asyncJobDao.insert(ModelUtil.buildAsyncJobDto(businessId, businessType, expectExecuteTime.getTime(), 0, AsyncJobStatus.READY, jobParam));
        } catch (DuplicateKeyException e) {
            log.warn("任务[{}]已存在", businessId);
        }

    }

    /**
     * 新增异步任务
     * @param asyncJobDtoList 异步任务列表
     */
    public void addJob(List<AsyncJobDto> asyncJobDtoList) {
        asyncJobDao.insert(asyncJobDtoList, 10000);
    }

}
