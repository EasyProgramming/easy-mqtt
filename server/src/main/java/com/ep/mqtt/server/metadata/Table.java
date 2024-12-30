package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2024/12/30 15:41
 */
public enum Table implements BaseEnum<String> {
    /**
     * 元数据表
     */
    META_DATA("meta_data", "元数据表"),;

    private final String code;

    private final String desc;

    Table(String code, String desc){
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
