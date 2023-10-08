package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2023/7/29 11:44
 */
public enum YesOrNo {

    /**
     * yes
     */
    YES(1),

    /**
     * no
     */
    NO(0);

    private final Integer value;

    YesOrNo(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static YesOrNo valueOf(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? YES : NO;
    }

    public static Boolean valueOf(Integer value) {
        if (value == null) {
            return null;
        }
        return YES.getValue().equals(value);
    }
}
