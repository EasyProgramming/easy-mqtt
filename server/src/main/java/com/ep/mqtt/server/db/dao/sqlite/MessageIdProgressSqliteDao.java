package com.ep.mqtt.server.db.dao.sqlite;

import com.ep.mqtt.server.db.dao.MessageIdProgressDao;
import org.apache.ibatis.annotations.Param;

/**
 * @author zbz
 * @date 2025/2/27 14:19
 */
public interface MessageIdProgressSqliteDao extends MessageIdProgressDao {

    /**
     * 进度自增
     * @param clientId 客户端id
     */
    @Override
    void incr(@Param("clientId") String clientId);

}