package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2024/4/10 14:35
 */
public enum RpcCommand implements BaseEnum<String> {

    /**
     * 清理已存在的session
     */
    CLEAN_EXIST_SESSION("CLEAN_EXIST_SESSION", "清理已存在的session"),

    /**
     * 发送消息
     */
    SEND_MESSAGE("SEND_MESSAGE", "发送消息"),
    ;

    private final String code;

    private final String desc;

    RpcCommand(String code, String desc) {
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
