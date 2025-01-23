package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ep.mqtt.server.metadata.AsyncJobBusinessType;
import com.ep.mqtt.server.metadata.AsyncJobExecuteResult;
import com.ep.mqtt.server.metadata.AsyncJobStatus;

import lombok.Data;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Data
@TableName(value = "async_job")
public class AsyncJobDto {
    @TableId
    private Long id;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 业务类型
     */
    private AsyncJobBusinessType businessType;

    /**
     * 最近一次执行开始时间
     */
    private Long lastStartTime;

    /**
     * 最近一次执行结束时间
     */
    private Long lastEndTime;

    /**
     * 最近一次执行结果的描述
     */
    private String lastExecuteResultDesc;

    /**
     * 最近一次执行结果
     */
    private AsyncJobExecuteResult lastExecuteResult;

    /**
     * 预计执行时间
     */
    private Long expectExecuteTime;

    /**
     * 执行次数
     */
    private Integer executeNum;

    /**
     * 执行状态
     */
    private AsyncJobStatus executeStatus;

    /**
     * 扩展数据json字符串
     */
    private String extendData;

    @Data
    public static class ExtendData {

        /**
         * 执行耗时
         */
        private Long executeCostTime;

    }

}