package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2025/3/8 14:11
 */
public enum DisconnectReason implements BaseEnum<String> {

    /**
     * 收到disconnect报文
     */
    NORMAL("NORMAL", "收到disconnect报文"),

    /**
     * 发生异常
     */
    EXCEPTION("EXCEPTION", "发生异常"),

    /**
     * 重复连接
     */
    REPEAT_CONNECT("REPEAT_CONNECT", "重复连接"),

    /**
     * 心跳超时
     */
    HEARTBEAT_TIMEOUT("HEARTBEAT_TIMEOUT", "心跳超时"),
    ;

    private final String code;

    private final String desc;

    DisconnectReason(String code, String desc){
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
