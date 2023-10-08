package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2023/8/11 10:53
 */
public enum LocalLockKey {

    /**
     * 管理主题过滤器
     */
    LOCK_MANAGE_TOPIC_FILTER("lock_manage_topic_filter:" + "%s"),

    /**
     * 管理保留消息
     */
    LOCK_MANAGE_RETAIN_MESSAGE("lock_manage_retain_message:" + "%s"),
    ;

    private final String key;

    LocalLockKey(String key) {
        this.key = key;
    }

    public String formatKey(Object... values) {
        return String.format(this.key, values);
    }

}
