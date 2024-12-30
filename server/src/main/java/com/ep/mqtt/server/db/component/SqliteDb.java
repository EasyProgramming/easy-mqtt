package com.ep.mqtt.server.db.component;

import com.ep.mqtt.server.metadata.DriverClass;
import com.ep.mqtt.server.metadata.Table;
import org.apache.ibatis.jdbc.SqlRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author zbz
 * @date 2024/12/28 15:03
 */
@Component
public class SqliteDb extends AbstractDb {

    @Override
    public void configDb() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 开启wal
        jdbcTemplate.execute("PRAGMA journal_mode=WAL;");

        // 完全同步，确保每次写入操作都被持久化
        jdbcTemplate.execute("PRAGMA synchronous=FULL;");
    }

    @Override
    public Boolean isInitTable() {
        SqlRunner sqlRunner = getSqlRunner();

        try {
            List<Map<String, Object>> objectMapList = sqlRunner.selectAll(
                    "SELECT * " +
                            "FROM sqlite_master " +
                            "WHERE type='table' " +
                            "AND name = ?",
                    Table.META_DATA.getCode()
            );

            return !CollectionUtils.isEmpty(objectMapList);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DriverClass driverClass() {
        return DriverClass.SQLITE;
    }

}
