package com.ep.mqtt.server.db.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.SendMessageDto;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Mapper
public interface SendMessageDao extends BaseMapper<SendMessageDto> {
    /**
     * 根据目标clientId删除
     *
     * @param toClientId
     *            客户端id
     */
    default void deleteByToClientId(String toClientId) {
        delete(Wrappers.lambdaQuery(SendMessageDto.class).eq(SendMessageDto::getToClientId, toClientId));
    }
}