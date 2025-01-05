package com.ep.mqtt.server.db.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.ClientSubscribeDto;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Mapper
public interface ClientSubscribeDao extends BaseMapper<ClientSubscribeDto> {

    /**
     * 根据clientId删除
     *
     * @param clientId
     *            客户端id
     */
    default void deleteByClientId(String clientId) {
        delete(Wrappers.lambdaQuery(ClientSubscribeDto.class).eq(ClientSubscribeDto::getClientId, clientId));
    }

}