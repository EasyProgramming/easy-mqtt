package com.ep.mqtt.server.metadata;

import org.apache.commons.lang3.StringUtils;

/**
 * @author zbz
 * @date 2024/4/10 14:35
 */
public enum RaftCommand {

    /**
     * 增加topic filter
     */
    ADD_TOPIC_FILTER,

    /**
     * 删除topic filter
     */
    REMOVE_TOPIC_FILTER,

    /**
     * 增加topic
     */
    ADD_TOPIC,

    /**
     * 删除topic
     */
    REMOVE_TOPIC,;

    public static RaftCommand get(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        try {
            return RaftCommand.valueOf(RaftCommand.class, name);
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

}
