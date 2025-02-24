package com.ep.mqtt.server.session;

import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

/**
 * @author zbz
 * @date 2023/7/16 15:49
 */
@Data
public class Session {

    private String clientId;

    private String sessionId;

    private Boolean isCleanSession;

    private Integer keepAliveTimeSeconds;

    private ChannelHandlerContext channelHandlerContext;

}
