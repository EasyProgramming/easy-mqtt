package com.ep.mqtt.server.server;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ep.mqtt.server.codec.MqttWebSocketCodec;
import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.deal.InboundDeal;
import com.ep.mqtt.server.handler.MqttMessageHandler;
import com.ep.mqtt.server.processor.AbstractMqttProcessor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/7/1 10:52
 */
@Slf4j
@Component
public class MqttServer {

    @Autowired
    private MqttServerProperties mqttServerProperties;

    @Autowired
    private List<AbstractMqttProcessor<?>> abstractMqttProcessorList;

    @Autowired
    private InboundDeal inboundDeal;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private SslContext sslContext;

    private Channel tcpChannel;

    private Channel websocketChannel;

    @PostConstruct
    public void start() throws Exception {
        log.info("init mqtt server");
        bossGroup = mqttServerProperties.getIsUseEpoll() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        workerGroup = mqttServerProperties.getIsUseEpoll() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        if (mqttServerProperties.getSsl() != null) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream sslCertificateFileInputStream =
                new FileInputStream(mqttServerProperties.getSsl().getSslCertificatePath())) {
                keyStore.load(sslCertificateFileInputStream,
                    mqttServerProperties.getSsl().getSslCertificatePassword().toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, mqttServerProperties.getSsl().getSslCertificatePassword().toCharArray());
            sslContext = SslContextBuilder.forServer(kmf).build();
        }
        tcpServer();
        websocketServer();
        log.info("start mqtt server, time:[{}]", System.currentTimeMillis());
    }

    @PreDestroy
    public void stop() {
        log.info("shutdown mqtt server, time:[{}]", System.currentTimeMillis());
        bossGroup.shutdownGracefully();
        bossGroup = null;
        workerGroup.shutdownGracefully();
        workerGroup = null;
        tcpChannel.closeFuture().syncUninterruptibly();
        tcpChannel = null;
        websocketChannel.closeFuture().syncUninterruptibly();
        websocketChannel = null;
        log.info("finish shutdown mqtt server, time:[{}]", System.currentTimeMillis());
    }

    private void tcpServer() throws Exception {
        ServerBootstrap sb = new ServerBootstrap();
        sb.group(bossGroup, workerGroup)
            .channel(
                mqttServerProperties.getIsUseEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
            // handler在初始化时就会执行
            .handler(new LoggingHandler(LogLevel.INFO))
            // childHandler会在客户端成功connect后才执行
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    ChannelPipeline channelPipeline = socketChannel.pipeline();
                    configSsl(socketChannel, channelPipeline);
                    channelPipeline.addLast("mqttDecoder", new MqttDecoder());
                    channelPipeline.addLast("mqttEncoder", MqttEncoder.INSTANCE);
                    channelPipeline.addLast("mqttMessageHandler",
                        new MqttMessageHandler(abstractMqttProcessorList, inboundDeal));
                }
            });
        tcpChannel = sb.bind(mqttServerProperties.getTcpPort()).sync().channel();
    }

    private void websocketServer() throws Exception {
        if (mqttServerProperties.getWebSocket() == null) {
            return;
        }

        ServerBootstrap sb = new ServerBootstrap();
        sb.group(bossGroup, workerGroup)
            .channel(
                mqttServerProperties.getIsUseEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
            // handler在初始化时就会执行
            .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    ChannelPipeline channelPipeline = socketChannel.pipeline();
                    // Netty提供的SSL处理
                    configSsl(socketChannel, channelPipeline);
                    // 将请求和应答消息编码或解码为HTTP消息
                    channelPipeline.addLast("http-codec", new HttpServerCodec());
                    // 将HTTP消息的多个部分合成一条完整的HTTP消息
                    channelPipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
                    // 将HTTP消息进行压缩编码
                    channelPipeline.addLast("compressor ", new HttpContentCompressor());
                    channelPipeline.addLast("protocol", new WebSocketServerProtocolHandler(
                        mqttServerProperties.getWebSocket().getWebsocketPath(), "mqtt,mqttv3.1,mqttv3.1.1", true, 65536));
                    channelPipeline.addLast("mqttWebSocketCodec", new MqttWebSocketCodec());
                    channelPipeline.addLast("mqttDecoder", new MqttDecoder());
                    channelPipeline.addLast("mqttEncoder", MqttEncoder.INSTANCE);
                    channelPipeline.addLast("mqttMessageHandler",
                        new MqttMessageHandler(abstractMqttProcessorList, inboundDeal));
                }
            });
        websocketChannel = sb.bind(mqttServerProperties.getWebSocket().getWebsocketPort()).sync().channel();
    }

    private void configSsl(SocketChannel socketChannel, ChannelPipeline channelPipeline) {
        if (mqttServerProperties.getSsl() != null) {
            SSLEngine sslEngine = sslContext.newEngine(socketChannel.alloc());
            // 服务端模式
            sslEngine.setUseClientMode(false);
            // 不需要验证客户端
            sslEngine.setNeedClientAuth(false);
            channelPipeline.addLast("ssl", new SslHandler(sslEngine));
        }
    }

}
