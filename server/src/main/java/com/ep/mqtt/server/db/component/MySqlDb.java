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

    public static String DB_TRANSACTION_ISOLATION = "READ-COMMITTED";

    public static String DB_CHARACTER_SET = "utf8mb4";

    public static String DB_COLLATION = "utf8mb4_general_ci";

    @Override
    public void configDb() {
        SqlRunner sqlRunner = getSqlRunner();

        try {
            List<Map<String, Object>> resultList = sqlRunner.selectAll("SELECT @@transaction_isolation");
            String transactionIsolation = resultList.get(0).get("@@transaction_isolation").toString();
            if (!DB_TRANSACTION_ISOLATION.equals(transactionIsolation)){
                throw new RuntimeException(String.format("MySql的隔离级别应为[%s]", DB_TRANSACTION_ISOLATION));
            }

            resultList = sqlRunner.selectAll("SHOW VARIABLES LIKE 'character_set_database'");
            String characterSet = resultList.get(0).get("Value").toString();
            if (!DB_CHARACTER_SET.equals(characterSet)){
                throw new RuntimeException(String.format("MySql的字符集应为[%s]", DB_CHARACTER_SET));
            }

            sqlRunner.selectAll("SHOW VARIABLES LIKE 'collation_database'");
            String collation = resultList.get(0).get("Value").toString();
            if (!DB_COLLATION.equals(collation)){
                throw new RuntimeException(String.format("MySql的排序规则应为[%s]", DB_COLLATION));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
