package com.ep.mqtt.server.db.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.ReceiveQos2MessageDto;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Mapper
public interface ReceiveQos2MessageDao extends BaseMapper<ReceiveQos2MessageDto> {

    /**
     * 根据来源clientId删除
     *
     * @param fromClientId
     *            客户端id
     */
    default void deleteByFromClientId(String fromClientId) {
        delete(Wrappers.lambdaQuery(ReceiveQos2MessageDto.class).eq(ReceiveQos2MessageDto::getFromClientId, fromClientId));
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
    default ReceiveQos2MessageDto getExistMessage(String fromClientId, String receivePacketId) {
        return selectOne(Wrappers.lambdaQuery(ReceiveQos2MessageDto.class).eq(ReceiveQos2MessageDto::getFromClientId, fromClientId)
            .eq(ReceiveQos2MessageDto::getReceivePacketId, receivePacketId));
    }

}