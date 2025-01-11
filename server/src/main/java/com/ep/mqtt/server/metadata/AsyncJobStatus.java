package com.ep.mqtt.server.metadata;

/**
 * 异步任务状态
 * @author zbz
 * @date 2025/1/8 17:24
 */
public enum AsyncJobStatus implements BaseEnum<String> {

    /**
     * 待执行
     */
    READY("READY", "待执行"),

    /**
     * 执行中
     */
    EXECUTING("EXECUTING", "执行中"),

    /**
     * 成功
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 失败
     */
    FAIL("FAIL", "失败"),

    ;

    private final String code;

    private final String desc;

    AsyncJobStatus(String code, String desc){
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
