package com.ep.mqtt.server.metadata;

/**
 * 异步任务业务类型
 * 
 * @author zbz
 * @date 2025/1/8 17:24
 */
public enum AsyncJobBusinessType implements BaseEnum<String> {

    /**
     * 分发消息
     */
    DISPATCH_MESSAGE("DISPATCH_MESSAGE", "分发消息", "DISPATCH_MESSAGE_%s"),

    /**
     * 生成messageId
     */
    GEN_MESSAGE_ID("GEN_MESSAGE_ID", "生成messageId", "GEN_MESSAGE_ID_%s"),

    /**
     * 清理任务
     */
    CLEAR_JOB("CLEAR_JOB", "清理任务", "CLEAR_JOB"),

    /**
     * 查询处理超时任务
     */
    QUERY_TIMEOUT_JOB("QUERY_TIMEOUT_JOB", "查询处理超时任务", "QUERY_TIMEOUT_JOB"),
    ;

    private final String code;

    private final String desc;

    private final String businessIdTemplate;

    AsyncJobBusinessType(String code, String desc, String businessIdTemplate) {
        this.code = code;
        this.desc = desc;
        this.businessIdTemplate = businessIdTemplate;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    public String getBusinessId(Object... values) {
        return String.format(this.businessIdTemplate, values);
    }
}
