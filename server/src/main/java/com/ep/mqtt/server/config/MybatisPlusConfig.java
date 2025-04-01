package com.ep.mqtt.server.config;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.ep.mqtt.server.metadata.BaseEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author zbz
 * @date 2024/7/13 15:52
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
            configuration.setCacheEnabled(false);
            configuration.setJdbcTypeForNull(JdbcType.NULL);
            configuration.setCallSettersOnNulls(true);
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.getTypeHandlerRegistry().setDefaultEnumTypeHandler(EnumTypeHandler.class);
        };
    }

    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return properties -> {
            properties.getGlobalConfig().getDbConfig().setIdType(IdType.AUTO);
            properties.getGlobalConfig().getDbConfig().setInsertStrategy(FieldStrategy.ALWAYS);
            properties.getGlobalConfig().getDbConfig().setUpdateStrategy(FieldStrategy.ALWAYS);
            properties.getGlobalConfig().getDbConfig().setWhereStrategy(FieldStrategy.ALWAYS);
        };
    }

    /**
     * 框架的枚举处理器
     * 
     * @param <T>
     *            对应枚举类型
     */
    public static class EnumTypeHandler<T extends Enum<T>> extends BaseTypeHandler<T> {

        private final Class<T> enumClassType;

        public EnumTypeHandler(Class<T> enumClassType) {
            BaseEnum.check(enumClassType);

            this.enumClassType = enumClassType;
        }

        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
            throws SQLException {
            if (jdbcType == null) {
                ps.setObject(i, this.getValue(parameter));
            } else {
                // see r3589
                ps.setObject(i, this.getValue(parameter), jdbcType.TYPE_CODE);
            }
        }

        @Override
        public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
            Object value = rs.getObject(columnName);
            if (null == value || rs.wasNull()) {
                return null;
            }

            return valueOf(value, enumClassType);
        }

        @Override
        public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            Object value = rs.getObject(columnIndex);
            if (null == value || rs.wasNull()) {
                return null;
            }

            return valueOf(value, enumClassType);
        }

        @Override
        public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            Object value = cs.getObject(columnIndex);
            if (null == value || cs.wasNull()) {
                return null;
            }

            return valueOf(value, enumClassType);
        }

        private T valueOf(Object dataBaseValue, Class<T> enumClassType) {
            return BaseEnum.getByCode(dataBaseValue, enumClassType);
        }

        private Object getValue(T object) {
            return BaseEnum.invokeGetMethod(object, BaseEnum.GET_CODE_METHOD_NAME);
        }
    }

}
