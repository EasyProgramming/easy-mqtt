package com.ep.mqtt.server.listener.msg;

import lombok.Data;

/**
 * @author zbz
 * @date 2023/9/2 10:32
 */
@Data
public class CleanExistSessionMsg {

    private String clientId;

    private String sessionId;

}
