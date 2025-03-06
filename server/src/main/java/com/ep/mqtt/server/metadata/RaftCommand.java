package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2024/4/10 14:35
 */
public enum RaftCommand implements BaseEnum<String> {

    /**
     * 清理已存在的session
     */
    CLEAN_EXIST_SESSION("CLEAN_EXIST_SESSION", "清理已存在的session"),

    /**
     * 增加topic filter
     */
    ADD_TOPIC_FILTER("ADD_TOPIC_FILTER", "增加topic filter"),

    /**
     * 删除topic filter
     */
    REMOVE_TOPIC_FILTER("REMOVE_TOPIC_FILTER", "删除topic filter"),

    /**
     * 发送消息
     */
    SEND_MESSAGE("SEND_MESSAGE", "发送消息"),

    /**
     * 增加retain message
     */
    ADD_RETAIN_MESSAGE("ADD_RETAIN_MESSAGE", "增加retain message"),

    /**
     * 删除retain message
     */
    REMOVE_RETAIN_MESSAGE("REMOVE_RETAIN_MESSAGE", "删除retain message"),
    ;

    private final String code;

    private final String desc;

    RaftCommand(String code, String desc) {
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
