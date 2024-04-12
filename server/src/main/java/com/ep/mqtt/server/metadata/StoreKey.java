package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2023/8/11 10:53
 */
public enum StoreKey {

    /**
     * 存储客户端基础信息
     */
    CLIENT_INFO_KEY("client_info", "hash"),

    /**
     * 存储客户端的订阅过滤器信息
     */
    CLIENT_TOPIC_FILTER_KEY("client_topic_filter" + Constant.STORE_KEY_SPLIT + "%s", "hash"),

    /**
     * 存储订阅过滤器信息
     */
    TOPIC_FILTER_KEY(Constant.TOPIC_FILTER_KEY_PREFIX + Constant.STORE_KEY_SPLIT + "%s", "hash"),

    /**
     * 存储消息
     */
    MESSAGE_KEY("message" + Constant.STORE_KEY_SPLIT + "%s", "hash"),

    /**
     * 存储rec消息
     */
    REC_MESSAGE_KEY("rec_message" + Constant.STORE_KEY_SPLIT + "%s", "hash"),

    /**
     * 存储rel消息
     */
    REL_MESSAGE_KEY("rel_message" + Constant.STORE_KEY_SPLIT + "%s", "set"),

    /**
     * 存储保留消息
     */
    RETAIN_MESSAGE_KEY("retain_message" + Constant.STORE_KEY_SPLIT + "%s", "string"),

    /**
     * 存储生成消息messageId进度
     */
    GEN_MESSAGE_ID_KEY("gen_message_id" + Constant.STORE_KEY_SPLIT + "%s", "string"),

    /**
     * topic filter 版本号
     */
    TOPIC_FILTER_VERSION_KEY("topic_filter_version", "string"),

    /**
     * topic filter 版本数据
     */
    TOPIC_FILTER_VERSION_DATA_KEY("topic_filter_version_data" + Constant.STORE_KEY_SPLIT + "%s", "string"),

    /**
     * topic 版本号
     */
    TOPIC_VERSION_KEY("topic_version", "string"),

    /**
     * topic 版本数据
     */
    TOPIC_VERSION_DATA_KEY("topic_version_data" + Constant.STORE_KEY_SPLIT + "%s", "string"),
    ;

    private final String key;

    private final String dataStructure;

    StoreKey(String key, String dataStructure) {
        this.key = key;
        this.dataStructure = dataStructure;
    }

    public String getDataStructure() {
        return dataStructure;
    }

    public String formatKey(Object... values) {
        return String.format(this.key, values);
    }

}
