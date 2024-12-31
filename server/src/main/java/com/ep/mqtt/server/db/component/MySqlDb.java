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
 * @date 2024/12/28 15:03
 */
@Component
public class MySqlDb extends AbstractDb {

    @Override
    public void configDb() {
        // TODO: 2024/12/31 根据元数据，检查数据库的隔离级别是否为rc
    }

    @Override
    public Boolean isInitTable() {
        SqlRunner sqlRunner = getSqlRunner();

        try {
            List<Map<String, Object>> objectMapList = sqlRunner.selectAll(
                    "SELECT * " +
                            "FROM information_schema.tables " +
                            "WHERE table_schema = ? " +
                            "AND table_name = ?",
                    getDatabase(), Table.META_DATA.getCode());

            return !CollectionUtils.isEmpty(objectMapList);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DriverClass driverClass() {
        return DriverClass.MYSQL;
    }

    private String getDatabase(){
        String jdbcUrl = dataSource.getJdbcUrl();
        int startIndex = jdbcUrl.lastIndexOf("/") + 1;
        int endIndex = jdbcUrl.contains("?") ? jdbcUrl.indexOf("?", startIndex) : jdbcUrl.length();

        return jdbcUrl.substring(startIndex, endIndex);
    }
}
