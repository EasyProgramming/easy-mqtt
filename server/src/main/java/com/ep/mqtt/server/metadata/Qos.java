package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2024/12/30 17:56
 */
public enum Qos implements BaseEnum<Integer> {
    /**
     * Qos=0
     */
    LEVEL_O(0, "Qos=0"),
    /**
     * Qos=1
     */
    LEVEL_1(1, "Qos=1"),
    /**
     * Qos=2
     */
    LEVEL_2(2, "Qos=2"),
    ;

    private final Integer code;

    private final String desc;

    Qos(Integer code, String desc){
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
