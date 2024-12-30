package com.ep.mqtt.server.metadata;

/**
 * 运行模式
 * @author zbz
 * @date 2024/12/30 16:35
 */
public enum RunMode implements BaseEnum<String> {
    /**
     * 单机模式
     */
    STANDALONE("standalone", "单机模式"),

    /**
     * 集群模式
     */
    CLUSTER("cluster", "集群模式");

    private final String code;

    private final String desc;

    RunMode(String code, String desc){
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
