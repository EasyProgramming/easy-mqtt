package com.ep.mqtt.server.util;

import org.apache.commons.lang3.StringUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

/**
 * @author zbz
 * @date 2023/7/21 10:48
 */
public class NettyUtil {

    private static final AttributeKey<String> CLIENT_ID_ATTR_KEY = AttributeKey.valueOf("clientId");

    private static final AttributeKey<String> REPEAT_ATTR_KEY = AttributeKey.valueOf("repeat");

    public static void setClientId(ChannelHandlerContext channelHandlerContext, String clientId) {
        channelHandlerContext.channel().attr(CLIENT_ID_ATTR_KEY).set(clientId);
    }

    public static String getClientId(ChannelHandlerContext channelHandlerContext) {
        return channelHandlerContext.channel().attr(CLIENT_ID_ATTR_KEY).get();
    }

    public static String getSessionId(ChannelHandlerContext channelHandlerContext) {
        return channelHandlerContext.channel().id().asLongText();
    }

    public static void setRepeat(ChannelHandlerContext channelHandlerContext) {
        channelHandlerContext.channel().attr(REPEAT_ATTR_KEY).set("repeat");
    }

    public static boolean isRepeat(ChannelHandlerContext channelHandlerContext) {
        return StringUtils.isNotBlank(channelHandlerContext.channel().attr(REPEAT_ATTR_KEY).get());
    }

}
