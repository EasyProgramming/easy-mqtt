package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2024/12/30 18:00
 */
public enum YesOrNo implements BaseEnum<String> {
    /**
     * 是
     */
    YES("Y", "是", 1),

    /**
     * 否
     */
    NO("N", "否", 0),
    ;

    private final String code;

    private final String desc;

    private final Integer number;

    YesOrNo(String code, String desc, Integer number){
        this.code = code;
        this.desc = desc;
        this.number = number;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    public Integer getNumber() {
        return number;
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
        return YES.getNumber().equals(value);
    }

}
