package com.ep.mqtt.server.config;

import com.ep.mqtt.server.metadata.BaseEnum;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.metadata.DriverClass;
import com.ep.mqtt.server.metadata.RunMode;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author zbz
 * @date 2024/12/4 11:56
 */
@Configuration
public class DataSourceConfig {

    @Resource
    private MqttServerProperties mqttServerProperties;

    /**
     * 生成数据库连接池
     * @return 数据库连接池
     */
    @Bean
    public HikariDataSource dataSource(){
        RunMode runMode = BaseEnum.getByCode(mqttServerProperties.getRunMode(), RunMode.class);
        if (runMode == null){
            throw new RuntimeException(String.format("不支持的运行模式[%s]", mqttServerProperties.getRunMode()));
        }

        HikariDataSource hikariDataSource;
        switch (runMode){
            case STANDALONE:
                hikariDataSource = new HikariDataSource();
                hikariDataSource.setJdbcUrl("jdbc:h2:" + Constant.PROJECT_BASE_DIR + "/database/easy-mqtt;DB_CLOSE_ON_EXIT=FALSE");
                hikariDataSource.setDriverClassName(DriverClass.H2.getCode());
                hikariDataSource.setMaximumPoolSize(Constant.PROCESSOR_NUM * 10);
                hikariDataSource.setIdleTimeout(Constant.PROCESSOR_NUM * 10);
                break;
            case CLUSTER:
                hikariDataSource = new HikariDataSource();
                hikariDataSource.setDriverClassName(DriverClass.MYSQL.getCode());

                if (mqttServerProperties.getDb() == null){
                    throw new RuntimeException("集群模式下，需要配置数据库信息");
                }
                String urlTemplate = "jdbc:mysql://%s:%s/%s?characterEncoding=UTF-8&rewriteBatchedStatements=true";
                hikariDataSource.setJdbcUrl(String.format(urlTemplate, mqttServerProperties.getDb().getHost(),
                        mqttServerProperties.getDb().getPort(), mqttServerProperties.getDb().getDatabase()));
                hikariDataSource.setUsername(mqttServerProperties.getDb().getUsername());
                hikariDataSource.setPassword(mqttServerProperties.getDb().getPassword());
                hikariDataSource.setMaximumPoolSize(Constant.PROCESSOR_NUM * 10);
                hikariDataSource.setIdleTimeout(Constant.PROCESSOR_NUM * 10);
                break;
            default:
                throw new RuntimeException(String.format("暂不支持该运行模式[%s]", mqttServerProperties.getRunMode()));
        }

        return hikariDataSource;
    }


}
