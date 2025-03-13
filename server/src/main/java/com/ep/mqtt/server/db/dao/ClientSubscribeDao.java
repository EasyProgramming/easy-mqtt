package com.ep.mqtt.server.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ep.mqtt.server.db.dto.ClientSubscribeDto;

import java.util.List;
import java.util.Set;

/**
 * @author zbz
 * @date 2024/12/30 17:37
 */
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

    /**
     * 获取客户端的订阅关系
     * @param clientId 客户端id
     * @param topicFilterSet 指定的topicFilter
     * @return 客户端的订阅关系
     */
    default List<ClientSubscribeDto> getClientSubscribe(String clientId, Set<String> topicFilterSet) {
        return selectList(Wrappers.lambdaQuery(ClientSubscribeDto.class).eq(ClientSubscribeDto::getClientId, clientId)
                .in(ClientSubscribeDto::getTopicFilter, topicFilterSet));
    }

    /**
     * 删除客户端订阅关系
     * @param clientId 客户端id
     * @param topicFilterSet 指定的topicFilter
     */
    default void deleteClientSubscribe(String clientId, Set<String> topicFilterSet) {
        delete(Wrappers.lambdaQuery(ClientSubscribeDto.class).eq(ClientSubscribeDto::getClientId, clientId).in(ClientSubscribeDto::getTopicFilter,
                topicFilterSet));
    }

    /**
     * 通过游标查询指定topic filter下的客户端
     * @param topicFilterSet 指定topic filter
     * @param cursor 游标
     * @param pageSize 页大小
     * @return 指定topic filter下的客户端
     */
    default List<ClientSubscribeDto> selectByCursor(Set<String> topicFilterSet, Long cursor, Integer pageSize){
        return selectList(Wrappers.lambdaQuery(ClientSubscribeDto.class).in(ClientSubscribeDto::getTopicFilter, topicFilterSet).ge(ClientSubscribeDto::getId,
                cursor).orderByAsc(ClientSubscribeDto::getId).last(" limit " + pageSize));
    }
}