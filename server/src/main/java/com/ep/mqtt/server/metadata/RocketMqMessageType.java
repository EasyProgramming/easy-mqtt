package com.ep.mqtt.server.metadata;

/**
 * rocketmq的消息类型
 * @author zbz
 * @date 2023/11/7 18:04
 */
public enum RocketMqMessageType {

    /**
     * 普通消息
     */
    NORMAL("normal", "普通消息"),

    /**
     * 全局/分区顺序消息
     */
    FIFO("fifo", "全局/分区顺序消息"),

    /**
     * 延时消息
     */
    DELAY("delay", "延时消息"),

    /**
     * 事务消息
     */
    TRANSACTION("transaction", "事务消息");


    private final String key;

    private final String desc;

    RocketMqMessageType(String key, String desc) {
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
