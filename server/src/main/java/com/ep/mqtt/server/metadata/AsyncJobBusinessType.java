package com.ep.mqtt.server.metadata;

/**
 * 异步任务业务类型
 * 
 * @author zbz
 * @date 2025/1/8 17:24
 */
public enum AsyncJobBusinessType implements BaseEnum<String> {

    /**
     * 发送publish报文
     */
    SEND_PUBLISH("SEND_PUBLISH", "发送publish报文"),

    ;

    private final String code;

    private final String desc;

    AsyncJobBusinessType(String code, String desc) {
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
