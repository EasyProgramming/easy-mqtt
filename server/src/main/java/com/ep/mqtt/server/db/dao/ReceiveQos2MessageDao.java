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
     * 根据客户端id、消息id删除
     * 
     * @param fromClientId
     *            客户端id
     * @param receivePacketId
     *            消息id
     * @return 是否删除成功
     */
    default boolean deleteByFromClientIdAndReceivePacketId(String fromClientId, Integer receivePacketId) {
        return delete(Wrappers.lambdaUpdate(ReceiveQos2MessageDto.class).eq(ReceiveQos2MessageDto::getFromClientId, fromClientId)
            .eq(ReceiveQos2MessageDto::getReceivePacketId, receivePacketId)) > 0;
    }

    /**
     * 根据客户端id、消息id查询
     *
     * @param fromClientId
     *            客户端id
     * @param receivePacketId
     *            消息id
     * @return 是否删除成功
     */
    default ReceiveQos2MessageDto selectByFromClientIdAndReceivePacketId(String fromClientId, Integer receivePacketId) {
        return selectOne(Wrappers.lambdaQuery(ReceiveQos2MessageDto.class).eq(ReceiveQos2MessageDto::getFromClientId, fromClientId)
            .eq(ReceiveQos2MessageDto::getReceivePacketId, receivePacketId));
    }

}