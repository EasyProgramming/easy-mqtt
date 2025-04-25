package com.ep.mqtt.server.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobExecuteResult;
import com.ep.mqtt.server.metadata.AsyncJobStatus;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Date;
import java.util.List;

/**
 * @author zbz
 * @date 2025/2/21 14:46
 */
public interface AsyncJobDao extends BaseMapper<AsyncJobDto> {

    /**
     * 获取待执行的任务
     *
     * @param size
     *            单次获取的任务数
     * @return 待执行任务列表
     */
    default List<AsyncJobDto> getPendingJob(Integer size) {
        return selectList(
                Wrappers.lambdaQuery(AsyncJobDto.class).eq(AsyncJobDto::getExecuteStatus, AsyncJobStatus.READY)
                        .le(AsyncJobDto::getExpectExecuteTime, System.currentTimeMillis())
                        .last("limit " + size));
    }

    /**
     * 尝试占用任务
     *
     * @param businessId
     *            业务id
     * @return 是否占用成功
     */
    default Boolean tryOccupyJob(String businessId) {
        return update(
                Wrappers.lambdaUpdate(AsyncJobDto.class).set(AsyncJobDto::getLastStartTime, System.currentTimeMillis())
                        .set(AsyncJobDto::getExecuteStatus, AsyncJobStatus.EXECUTING).eq(AsyncJobDto::getBusinessId, businessId)
                        .eq(AsyncJobDto::getExecuteStatus, AsyncJobStatus.READY)) > 0;
    }

    /**
     * 对任务加锁
     *
     * @param businessId
     *            业务id
     * @return 任务数据
     */
    AsyncJobDto lock(String businessId);

    /**
     * 完成任务
     *
     * @param businessId
     *            业务id
     * @param jobStatus
     *            任务状态
     * @param executeNum
     *            执行次数
     * @param asyncJobExecuteResult
     *            执行结果
     * @param executeResultDesc
     *            执行结果描述
     * @param expectExecuteTime
     *            预计执行时间
     */
    default void finishJob(String businessId, AsyncJobStatus jobStatus, Integer executeNum,
                           AsyncJobExecuteResult asyncJobExecuteResult, String executeResultDesc, Long expectExecuteTime) {
        update(Wrappers.lambdaUpdate(AsyncJobDto.class).set(AsyncJobDto::getExecuteStatus, jobStatus)
                .set(AsyncJobDto::getExecuteNum, executeNum).set(AsyncJobDto::getLastExecuteResult, asyncJobExecuteResult)
                .set(AsyncJobDto::getLastExecuteResultDesc, executeResultDesc)
                .set(expectExecuteTime != null, AsyncJobDto::getExpectExecuteTime, expectExecuteTime)
                .set(AsyncJobDto::getLastEndTime, System.currentTimeMillis()).eq(AsyncJobDto::getBusinessId, businessId));
    }

    /**
     * 获取执行超时的任务
     *
     * @param size
     *            单次获取的任务数
     * @param now
     *            当前时间
     * @return 执行超时的任务列表
     */
    default List<AsyncJobDto> getExecuteTimeoutJob(Integer size, Date now) {
        return selectList(
            Wrappers.lambdaQuery(AsyncJobDto.class).lt(AsyncJobDto::getLastStartTime, DateUtils.addDays(now, -1))
                        .eq(AsyncJobDto::getExecuteStatus, AsyncJobStatus.EXECUTING).last("limit " + size));
    }

    /**
     * 获取执行成功的任务
     *
     * @param size
     *            单次获取的任务数
     * @param now
     *            当前时间
     * @return 执行成功的任务列表
     */
    default List<AsyncJobDto> getExecuteSuccessJob(Integer size, Date now) {
        return selectList(Wrappers.lambdaQuery(AsyncJobDto.class).lt(AsyncJobDto::getLastStartTime, DateUtils.addDays(now, -1))
            .eq(AsyncJobDto::getExecuteStatus, AsyncJobStatus.FINISH).eq(AsyncJobDto::getLastExecuteResult, AsyncJobExecuteResult.SUCCESS)
            .last("limit " + size));
    }
}
