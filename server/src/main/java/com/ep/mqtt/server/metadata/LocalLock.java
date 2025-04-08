package com.ep.mqtt.server.metadata;

/**
 * @author : zbz
 * @date : 2025/4/4
 */
public enum LocalLock implements BaseEnum<String>{
    /**
     * 加锁客户端
     */
    LOCK_CLIENT("lock:client:%s", "加锁客户端"),

    ;

    private final String code;

    private final String desc;

    LocalLock(String code, String desc){
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

    public String getLocalLockName(Object... values) {
        return String.format(this.code, values).intern();
    }
}
