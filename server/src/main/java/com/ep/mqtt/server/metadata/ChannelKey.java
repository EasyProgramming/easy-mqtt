package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2023/8/11 10:53
 */
public enum ChannelKey {

    /**
     * 发送消息
     */
    SEND_MESSAGE("send_message", "发送消息"),

    CLEAR_EXIST_SESSION("clear_exist_session", "清理已存在的session"),
    ;

    private final String key;

    private final String desc;

    ChannelKey(String key, String desc) {
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
