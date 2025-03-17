package com.ep.mqtt.server.config;

import com.ep.mqtt.server.db.component.AbstractDb;
import com.ep.mqtt.server.metadata.BaseEnum;
import com.ep.mqtt.server.metadata.DriverClass;
import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;
import java.util.List;

/**
 * @author zbz
 * @date 2024/12/24 17:50
 */
@Configuration
public class InitDbConfig {

    public InitDbConfig(HikariDataSource dataSource, List<AbstractDb> abstractDbList){
        DriverClass driverClass = BaseEnum.getByCode(dataSource.getDriverClassName(), DriverClass.class);
        if (driverClass == null){
            throw new RuntimeException(String.format("不支持该驱动[%s]", dataSource.getDriverClassName()));
        }

        for (AbstractDb abstractDb : abstractDbList){
            if (!abstractDb.driverClass().equals(driverClass)){
                continue;
            }

            try {
                abstractDb.start();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return;
        }

        throw new RuntimeException(String.format("缺失该驱动[%s]的实现类", dataSource.getDriverClassName()));
    }

//    @ConditionalOnProperty(prefix = "mqtt.server", value = "run-mode", havingValue = "standalone")
//    @Configuration
//    @MapperScan(value = {"com.ep.mqtt.server.db.dao.sqlite"})
//    public static class SqliteMapperScan {
//    }

    @ConditionalOnProperty(prefix = "mqtt.server", value = "run-mode", havingValue = "standalone")
    @Configuration
    @MapperScan(value = {"com.ep.mqtt.server.db.dao.h2"})
    public static class H2MapperScan {
    }

    @ConditionalOnProperty(prefix = "mqtt.server", value = "run-mode", havingValue = "cluster")
    @Configuration
    @MapperScan(value = {"com.ep.mqtt.server.db.dao.mysql"})
    public static class MySqlMapperScan {
    }

}
