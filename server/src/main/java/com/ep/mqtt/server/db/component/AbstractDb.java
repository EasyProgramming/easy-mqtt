package com.ep.mqtt.server.db.component;

import com.ep.mqtt.server.metadata.DriverClass;
import com.ep.mqtt.server.util.ScriptRunner;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.jdbc.SqlRunner;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;

/**
 * @author zbz
 * @date 2024/12/28 11:56
 */
public abstract class AbstractDb {

    public static final String INIT_SQL_FILE_PATH = "db_script/init/%s/init.sql";

    @Resource
    protected HikariDataSource dataSource;

    public void start() throws SQLException {
        configDb();

        if (isInitTable()){
            return;
        }

        ClassPathResource classPathResource = new ClassPathResource(String.format(INIT_SQL_FILE_PATH, driverClass().getSimpleDbName()));
        try(
                InputStream inputStream = classPathResource.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream)
        ) {
            ScriptRunner scriptRunner = new ScriptRunner(dataSource.getConnection());
            scriptRunner.setStopOnError(true);

            scriptRunner.runScript(inputStreamReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected SqlRunner getSqlRunner(){
        try {
            return new SqlRunner(dataSource.getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 配置db的参数
     */
    public abstract void configDb();

    /**
     * 是否初始化数据表
     * @return 是否初始化
     */
    public abstract Boolean isInitTable();

    /**
     * 获取驱动的类路径
     * @return 驱动的类路径
     */
    public abstract DriverClass driverClass();

}
