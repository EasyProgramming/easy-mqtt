package com.ep.mqtt.server.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.YesOrNo;

import java.util.List;

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
    default boolean updateSendPacketId(Long sendMessageId, Integer sendPacketId) {
        return update(Wrappers.lambdaUpdate(SendMessageDto.class).set(SendMessageDto::getSendPacketId, sendPacketId).eq(SendMessageDto::getId,
                sendMessageId).isNull(SendMessageDto::getSendPacketId)) > 0;
    }

    /**
     * 查询qos=1的消息
     *
     * @param toClientId
     *            客户端id
     * @param sendPacketId
     *            消息id
     */
    default List<SendMessageDto> selectAtLeastOnceMessage(String toClientId, Integer sendPacketId) {
        return selectList(Wrappers.lambdaQuery(SendMessageDto.class).eq(SendMessageDto::getToClientId, toClientId)
                .eq(SendMessageDto::getSendPacketId, sendPacketId).eq(SendMessageDto::getSendQos, Qos.LEVEL_1));
    }


    /**
     * 更新已收到pubRec报文
     * 
     * @param sendMessageId
     *            发送消息id
     */
    default void updateReceivePubRec(Long sendMessageId) {
        update(Wrappers.lambdaUpdate(SendMessageDto.class)
                .set(SendMessageDto::getIsReceivePubRec, YesOrNo.YES)
                .eq(SendMessageDto::getId, sendMessageId)
                .eq(SendMessageDto::getIsReceivePubRec, YesOrNo.NO)
        );
    }

    /**
     * 查询未收到pubRec报文
     *
     * @param toClientId
     *            客户端id
     * @param sendPacketId
     *            消息id
     */
    default List<SendMessageDto> selectUnReceivePubRec(String toClientId, Integer sendPacketId) {
        return selectList(Wrappers.lambdaQuery(SendMessageDto.class)
                .eq(SendMessageDto::getToClientId, toClientId).eq(SendMessageDto::getSendPacketId, sendPacketId)
                .eq(SendMessageDto::getIsReceivePubRec, YesOrNo.NO)
                .eq(SendMessageDto::getSendQos, Qos.LEVEL_2));
    }

    /**
     * 查询重试的消息
     * 
     * @param toClientId
     *            客户端id
     * @return 重试的消息
     */
    default List<SendMessageDto> selectRetryMessage(String toClientId) {
        return selectList(Wrappers.lambdaQuery(SendMessageDto.class).eq(SendMessageDto::getToClientId, toClientId));
    }

    /**
     * 删除qos=2的消息
     *
     * @param toClientId
     *            客户端id
     * @param sendPacketId
     *            消息id
     */
    default void deleteExactlyOnceMessage(String toClientId, Integer sendPacketId) {
        delete(Wrappers.lambdaQuery(SendMessageDto.class).eq(SendMessageDto::getToClientId, toClientId)
                .eq(SendMessageDto::getIsReceivePubRec, YesOrNo.YES)
                .eq(SendMessageDto::getSendPacketId, sendPacketId).eq(SendMessageDto::getSendQos, Qos.LEVEL_2));
    }

    /**
     * 查询qos=2的消息
     *
     * @param toClientId
     *            客户端id
     * @param sendPacketId
     *            消息id
     */
    default List<SendMessageDto> selectExactlyOnceMessage(String toClientId, Integer sendPacketId) {
        return selectList(Wrappers.lambdaQuery(SendMessageDto.class).eq(SendMessageDto::getToClientId, toClientId)
                .eq(SendMessageDto::getIsReceivePubRec, YesOrNo.YES)
                .eq(SendMessageDto::getSendPacketId, sendPacketId).eq(SendMessageDto::getSendQos, Qos.LEVEL_2));
    }
}