package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2024/12/27 14:33
 */
public enum DriverClass implements BaseEnum<String> {
    /**
     * sqlite的驱动
     */
    SQLITE("org.sqlite.JDBC", "sqlite的驱动", "sqlite"),

    /**
     * mysql的驱动
     */
    MYSQL("com.mysql.cj.jdbc.Driver", "mysql的驱动", "mysql"),

    /**
     * h2的驱动
     */
    H2("org.h2.Driver", "h2的驱动", "h2"),
    ;

    private final String code;

    private final String desc;

    private final String simpleDbName;

    DriverClass(String code, String desc, String simpleDbName){
        this.code = code;
        this.desc = desc;
        this.simpleDbName = simpleDbName;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    public String getSimpleDbName() {
        return simpleDbName;
    }
}
