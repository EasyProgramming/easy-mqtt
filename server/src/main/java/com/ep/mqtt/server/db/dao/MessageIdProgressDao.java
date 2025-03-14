package com.ep.mqtt.server.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.MessageIdProgressDto;
import org.apache.ibatis.annotations.Param;

/**
 * @author zbz
 * @date 2025/2/27 14:19
 */
public interface MessageIdProgressDao extends BaseMapper<MessageIdProgressDto> {

    /**
     * 生成消息id
     * @param clientId 客户端id
     * @return 消息id
     */
    default Integer genMessageId(String clientId) {
        incr(clientId);
        MessageIdProgressDto messageIdProgressDto = selectOne(Wrappers.lambdaQuery(MessageIdProgressDto.class).eq(MessageIdProgressDto::getClientId
                , clientId));

        return messageIdProgressDto == null ? null : (int)(messageIdProgressDto.getProgress() % 65535);
    }

    /**
     * 进度自增
     * @param clientId 客户端id
     */
    void incr(@Param("clientId") String clientId);

    /**
     * 根据客户端id删除
     * @param clientId 客户端id
     */
    default void deleteByClientId(String clientId){
        delete(Wrappers.lambdaUpdate(MessageIdProgressDto.class).eq(MessageIdProgressDto::getClientId, clientId));
    }
}