package com.ep.mqtt.server.db.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Data
@TableName(value = "meta_data")
public class MetaDataDto {
    @TableId(type = IdType.ASSIGN_ID)
    private String key;

    private String value;

    private String desc;

}