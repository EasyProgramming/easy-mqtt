package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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

    private String jobId;

    private String businessType;

    private Long lastStartTime;

    private Long lastEndTime;

    private Long nextExecuteTime;

    private Integer executeNum;

    private String executeStatus;

    private String executeResult;

    private String extendData;

}