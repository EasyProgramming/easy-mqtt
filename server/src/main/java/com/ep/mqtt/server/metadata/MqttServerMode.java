package com.ep.mqtt.server.metadata;

/**
 * mqtt服务器的模式
 * @author zbz
 * @date 2023/8/11 10:53
 */
public enum MqttServerMode {

    /**
     * 默认
     */
    DEFAULT("default", "默认"),

    /**
     * 阿里云
     */
    ALIYUN("aliyun", "阿里云"),;

    private final String key;

    private final String desc;

    MqttServerMode(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public String getKey() {
        return key;
    }

}
