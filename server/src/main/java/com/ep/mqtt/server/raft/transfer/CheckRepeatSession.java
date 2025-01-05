package com.ep.mqtt.server.raft.transfer;

import lombok.Data;

/**
 * @author : zbz
 * @date : 2025/1/1
 */
@Data
public class CheckRepeatSession {

    private String clientId;

    private String sessionId;

}
