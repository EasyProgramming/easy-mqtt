package com.ep.mqtt.server.db.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ep.mqtt.server.db.dto.ReceiveQos2MessageUniqueDto;

/**
 * @author : zbz
 * @date : 2025/2/2
 */
@Mapper
public interface ReceiveQos2MessageUniqueDao extends BaseMapper<ReceiveQos2MessageUniqueDto> {}
