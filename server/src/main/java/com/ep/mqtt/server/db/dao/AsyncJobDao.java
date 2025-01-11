package com.ep.mqtt.server.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.AsyncJobDto;
import com.ep.mqtt.server.metadata.AsyncJobStatus;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Mapper
public interface AsyncJobDao extends BaseMapper<AsyncJobDto> {

    /**
     * 获取待执行的任务
     * @param size 单次获取的任务数
     * @return 待执行任务列表
     */
    default List<AsyncJobDto> getPendingJob(Integer size){
        return selectList(
                Wrappers.lambdaQuery(AsyncJobDto.class)
                        .eq(AsyncJobDto::getExecuteStatus, AsyncJobStatus.READY)
                        .le(AsyncJobDto::getExpectExecuteTime, System.currentTimeMillis())
                        .orderByAsc(AsyncJobDto::getId)
                        .last("limit " + size)
        );
    }

    /**
     * 尝试占用任务
     * @param businessId 业务id
     * @return 是否占用成功
     */
    default Boolean tryOccupyJob(String businessId){
        return update(
                Wrappers.lambdaUpdate(AsyncJobDto.class)
                        .set(AsyncJobDto::getLastStartTime, System.currentTimeMillis())
                        .set(AsyncJobDto::getExecuteStatus, AsyncJobStatus.EXECUTING)
                        .eq(AsyncJobDto::getBusinessId, businessId)
                        .eq(AsyncJobDto::getExecuteStatus, AsyncJobStatus.READY)
        ) > 0;
    }

    /**
     * 对任务加锁
     * @param businessId 业务id
     * @return 任务数据
     */
    default AsyncJobDto lock(String businessId){
        update(
                Wrappers.lambdaUpdate(AsyncJobDto.class)
                        .set(AsyncJobDto::getBusinessId, businessId)
                        .eq(AsyncJobDto::getBusinessId, businessId)
        );

        return selectOne(Wrappers.lambdaUpdate(AsyncJobDto.class).eq(AsyncJobDto::getBusinessId, businessId));
    }

    /**
     * 完成任务
     * @param businessId 业务id
     * @param jobStatus 任务状态
     * @param executeResult 执行结果
     * @param executeNum 执行次数
     * @param nextExecuteTime 下一次执行时间
     * @param extendData 扩展数据
     */
    default void finishJob(String businessId, AsyncJobStatus jobStatus, String executeResult, Integer executeNum, Long nextExecuteTime,
                           AsyncJobDto.ExtendData extendData){

    }
}