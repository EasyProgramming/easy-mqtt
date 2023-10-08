package com.ep.mqtt.server.session;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.TimeoutUtils;

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

    public Long getDataExpireTime() {
        return getKeepAliveTimeSeconds() * 3L;
    }

    public TimeUnit getDataExpireTimeUnit() {
        return TimeUnit.SECONDS;
    }

    public Long getDataExpireTimeMilliSecond() {
        return TimeoutUtils.toMillis(getDataExpireTime(), getDataExpireTimeUnit());
    }
}
