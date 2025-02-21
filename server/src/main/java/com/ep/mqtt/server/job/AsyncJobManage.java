package com.ep.mqtt.server.job;

import java.util.Date;

import javax.annotation.Resource;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.ep.mqtt.server.db.dao.sqlite.AsyncJobSqliteDao;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobStatus;
import com.ep.mqtt.server.util.JsonUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author : zbz
 * @date : 2025/1/24
 */
@Slf4j
@Service
public class AsyncJobManage {

    @Resource
    private AsyncJobSqliteDao asyncJobDao;

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
            AsyncJobDto asyncJobDto = new AsyncJobDto();
            asyncJobDto.setBusinessId(businessId);
            asyncJobDto.setBusinessType(businessType);
            asyncJobDto.setJobParam(JsonUtil.obj2String(jobParam));
            asyncJobDto.setExpectExecuteTime(expectExecuteTime.getTime());
            asyncJobDto.setExecuteNum(0);
            asyncJobDto.setExecuteStatus(AsyncJobStatus.READY);

            asyncJobDao.insert(asyncJobDto);
        } catch (DuplicateKeyException e) {
            log.warn("任务[{}]已存在", businessId);
        }

    }

}
