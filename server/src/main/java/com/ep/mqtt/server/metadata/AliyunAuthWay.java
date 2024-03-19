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
    TOKEN("token", "Token", "token鉴权模式"),;

    private final String key;

    private final String desc;

    private final String aliyunKey;

    AliyunAuthWay(String key, String aliyunKey, String desc) {
        this.key = key;
        this.desc = desc;
        this.aliyunKey = aliyunKey;
    }

    public String getDesc() {
        return desc;
    }

    public String getKey() {
        return key;
    }

    public String getAliyunKey() {
        return aliyunKey;
    }
}
