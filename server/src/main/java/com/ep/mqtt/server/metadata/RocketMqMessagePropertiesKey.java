package com.ep.mqtt.server.metadata;

/**
 * rocketmq的
 * @author zbz
 * @date 2023/11/7 18:04
 */
public enum RocketMqMessagePropertiesKey {

    /**
     * QoS
     */
    QOS_LEVEL("qoslevel", "QoS"),

    /**
     * MQTT子级Topic
     */
    MQTT_SECOND_TOPIC("mqttSecondTopic", "具体的子级Topic字符串"),

    /**
     * clientId
     */
    CLIENT_ID("clientId", "具体的Client ID字符串");


    private final String key;

    private final String desc;

    RocketMqMessagePropertiesKey(String key, String desc) {
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
