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

    /**
     * 管理topicFilter
     */
    MANAGE_TOPIC_FILTER("manage_topic_filter", "管理topicFilter"),

    CLEAR_EXIST_SESSION("clear_exist_session", "清理已存在的session"),

    /**
     * 管理保留消息
     */
    MANAGE_RETAIN_MESSAGE("manage_retain_message", "管理保留消息"),;

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
