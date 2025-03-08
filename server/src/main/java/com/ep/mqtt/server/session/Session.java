package com.ep.mqtt.server.session;

import com.ep.mqtt.server.metadata.Qos;
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

    private WillMessage willMessage;

    @Data
    public static class WillMessage {

        private Qos qos;

        private String topic;

        private Boolean isRetain;

        private String payload;

    }

}
