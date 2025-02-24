package com.ep.mqtt.server.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

/**
 * @author zbz
 * @date 2023/7/21 10:48
 */
public class NettyUtil {

    private static final AttributeKey<String> CLIENT_ID_ATTR_KEY = AttributeKey.valueOf("clientId");

    private static final AttributeKey<String> CLEAN_DATA_REASON_ATTR_KEY = AttributeKey.valueOf("cleanDataReason");

    public static void setClientId(ChannelHandlerContext channelHandlerContext, String clientId) {
        channelHandlerContext.channel().attr(CLIENT_ID_ATTR_KEY).set(clientId);
    }

    public static String getClientId(ChannelHandlerContext channelHandlerContext) {
        return channelHandlerContext.channel().attr(CLIENT_ID_ATTR_KEY).get();
    }

    public static String getSessionId(ChannelHandlerContext channelHandlerContext) {
        return channelHandlerContext.channel().id().asLongText();
    }

    public static void setCleanDataReason(ChannelHandlerContext channelHandlerContext, String reason) {
        channelHandlerContext.channel().attr(CLEAN_DATA_REASON_ATTR_KEY).set(reason);
    }

    public static String getCleanDataReason(ChannelHandlerContext channelHandlerContext) {
        return channelHandlerContext.channel().attr(CLEAN_DATA_REASON_ATTR_KEY).get();
    }

}
