package com.ep.mqtt.server.db.component;

import com.ep.mqtt.server.metadata.DriverClass;
import com.ep.mqtt.server.metadata.Table;
import org.apache.ibatis.jdbc.SqlRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author zbz
 * @date 2025/3/17 13:43
 */
@Component
public class H2Db extends AbstractDb {
    @Override
    public void configDb() {

    }

    @Override
    public Boolean isInitTable() {
        SqlRunner sqlRunner = getSqlRunner();

        try {
            List<Map<String, Object>> objectMapList = sqlRunner.selectAll(
                    "SELECT * " +
                            "FROM INFORMATION_SCHEMA.TABLES  " +
                            "WHERE TABLE_SCHEMA  = ? " +
                            "AND TABLE_NAME = ?",
                    "PUBLIC", Table.META_DATA.getCode().toUpperCase());

            return !CollectionUtils.isEmpty(objectMapList);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DriverClass driverClass() {
        return DriverClass.H2;
    }
}
