package com.ep.mqtt.server.metadata;

/**
 * 阿里云鉴权方式
 * @author zbz
 * @date 2023/10/31 12:01
 */
public enum AliyunAuthWay {

    /**
     * token
     */
    TOKEN("token", "token鉴权模式"),;

    private final String key;

    private final String desc;

    AliyunAuthWay(String key, String desc) {
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
