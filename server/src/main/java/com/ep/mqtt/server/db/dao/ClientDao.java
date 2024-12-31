package com.ep.mqtt.server.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.ClientDto;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
@Mapper
public interface ClientDao extends BaseMapper<ClientDto> {

    /**
     * 根据clientId获取client
     * @param clientId 客户端id
     * @return 客户端信息
     */
    default ClientDto selectByClientId(String clientId){
        return this.selectOne(Wrappers.lambdaQuery(ClientDto.class).eq(ClientDto::getClientId, clientId));
    }

}