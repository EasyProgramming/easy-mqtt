package com.ep.mqtt.server.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
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

    /**
     * 更新sendPacketId
     * @param sendMessageId 记录id
     * @param sendPacketId 消息id
     * @return 是否更新成功
     */
    default boolean updateSendPacketId(Long sendMessageId, String sendPacketId){
        return update(Wrappers.lambdaUpdate(SendMessageDto.class).set(SendMessageDto::getSendPacketId, sendPacketId).eq(SendMessageDto::getId,
                sendMessageId).isNull(SendMessageDto::getSendPacketId)) > 0;
    }

    /**
     * 删除qos=1的消息
     * 
     * @param toClientId
     *            客户端id
     * @param sendPacketId
     *            消息id
     */
    default void deleteAtLeastOnceMessage(String toClientId, String sendPacketId) {
        delete(Wrappers.lambdaQuery(SendMessageDto.class).eq(SendMessageDto::getToClientId, toClientId)
            .eq(SendMessageDto::getSendPacketId, sendPacketId).eq(SendMessageDto::getSendQos, Qos.LEVEL_1));
    }

    /**
     * 更新已收到pubRec报文
     * 
     * @param toClientId
     *            客户端id
     * @param sendPacketId
     *            消息id
     * @return 更新结果
     */
    default boolean updateReceivePubRec(String toClientId, String sendPacketId) {
        return update(Wrappers.lambdaUpdate(SendMessageDto.class).set(SendMessageDto::getIsReceivePubRec, YesOrNo.YES)
            .eq(SendMessageDto::getToClientId, toClientId).eq(SendMessageDto::getSendPacketId, sendPacketId)
            .eq(SendMessageDto::getSendQos, Qos.LEVEL_2)) > 0;
    }
}