package com.ep.mqtt.server.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.ClientDto;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
public interface ClientDao extends BaseMapper<ClientDto> {

    /**
     * 根据clientId获取
     * 
     * @param clientId
     *            客户端id
     * @return 客户端信息
     */
    default ClientDto selectByClientId(String clientId) {
        return this.selectOne(Wrappers.lambdaQuery(ClientDto.class).eq(ClientDto::getClientId, clientId));
    }

    /**
     * 根据clientId删除
     * 
     * @param clientId
     *            客户端id
     */
    default void deleteByClientId(String clientId) {
        delete(Wrappers.lambdaQuery(ClientDto.class).eq(ClientDto::getClientId, clientId));
    }

    /**
     * 更新连接时间
     * 
     * @param clientId
     *            客户端id
     * @param connectTime
     *            连接时间
     */
    default void updateConnectTime(String clientId, Long connectTime) {
        this.update(Wrappers.lambdaUpdate(ClientDto.class).set(ClientDto::getLastConnectTime, connectTime).eq(ClientDto::getClientId, clientId));
    }

    /**
     * 对客户端加锁
     * @param clientId 客户端id
     * @return 客户端数据
     */
    default ClientDto lock(String clientId){
        return selectOne(Wrappers.lambdaUpdate(ClientDto.class).eq(ClientDto::getClientId, clientId).last("for update"));
    }

}