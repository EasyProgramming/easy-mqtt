package com.ep.mqtt.server.config;

import com.ep.mqtt.server.metadata.Constant;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.tomcat.util.bcel.Const;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * @author zbz
 * @date 2024/12/4 11:56
 */
@Configuration
public class SqliteConfig {

    @Bean
    public DataSource dataSource(){
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl("jdbc:sqlite:" + Constant.PROJECT_BASE_DIR + "/easy-mqtt.db");
        hikariDataSource.setDriverClassName("org.sqlite.JDBC");

        setSqliteConfig(hikariDataSource);

        return hikariDataSource;
    }


    private void setSqliteConfig(DataSource dataSource){
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 开启wal
        jdbcTemplate.execute("PRAGMA journal_mode=WAL;");

        // 完全同步，确保每次写入操作都被持久化
        jdbcTemplate.execute("PRAGMA synchronous=FULL;");
    }

}
