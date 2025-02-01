package com.ep.mqtt.server.db.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.ReceiveMessageDto;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Mapper
public interface ReceiveMessageDao extends BaseMapper<ReceiveMessageDto> {

    /**
     * 根据来源clientId删除
     *
     * @param fromClientId
     *            客户端id
     */
    default void deleteByFromClientId(String fromClientId) {
        delete(Wrappers.lambdaQuery(ReceiveMessageDto.class).eq(ReceiveMessageDto::getFromClientId, fromClientId));
    }

    /**
     * 获取已存在的消息
     * 
     * @param fromClientId
     *            来源的clientId
     * @param receivePacketId
     *            收到的标识符
     * @return 命中的消息
     */
    default ReceiveMessageDto getExistMessage(String fromClientId, String receivePacketId) {
        return selectOne(Wrappers.lambdaQuery(ReceiveMessageDto.class).eq(ReceiveMessageDto::getFromClientId, fromClientId)
            .eq(ReceiveMessageDto::getReceivePacketId, receivePacketId));
    }

}