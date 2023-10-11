package com.ep.mqtt.server.util;

import io.netty.channel.ChannelHandlerContext;
import io.vertx.core.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author zbz
 * @date 2023/7/25 17:09
 */
@Slf4j
public class WorkerThreadPool {

    public static Vertx VERTX;

    private static WorkerExecutor DEFAULT_WORKER_EXECUTOR;

    private static WorkerExecutor DEAL_MESSAGE_WORKER_EXECUTOR;

    private static final Long MAX_EXECUTE_TIME = 20L;

    private static final TimeUnit MAX_EXECUTE_TIME_UNIT = TimeUnit.MILLISECONDS;

    public static void init(Integer workerThreadPoolSize){
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setWarningExceptionTime(MAX_EXECUTE_TIME * 2);
        vertxOptions.setWarningExceptionTimeUnit(MAX_EXECUTE_TIME_UNIT);
        VERTX = Vertx.vertx(vertxOptions);
        DEFAULT_WORKER_EXECUTOR =
                VERTX.createSharedWorkerExecutor("worker-thread-pool", workerThreadPoolSize, MAX_EXECUTE_TIME, MAX_EXECUTE_TIME_UNIT);
        DEAL_MESSAGE_WORKER_EXECUTOR =
                VERTX.createSharedWorkerExecutor("deal-message-thread-pool", workerThreadPoolSize, MAX_EXECUTE_TIME, MAX_EXECUTE_TIME_UNIT);
    }

    public static <T> void execute(Consumer<Promise<T>> blockingHandlerConsumer) {
        Handler<Promise<T>> blockingCodeHandler = (promise)->{
            blockingHandlerConsumer.accept(promise);
            promise.complete();
        };
        execute(blockingCodeHandler, null, DEFAULT_WORKER_EXECUTOR);
    }


    public static <T> void dealMessage(Consumer<Promise<T>> blockingHandlerConsumer, Runnable resultHandlerRunnable,
                                   ChannelHandlerContext channelHandlerContext) {
        Handler<Promise<T>> blockingCodeHandler = (promise)->{
            blockingHandlerConsumer.accept(promise);
            promise.complete();
        };
        Handler<AsyncResult<T>> asyncResultHandler = result -> {
            if (result.cause() != null) {
                log.error("deal message execute error, client id: [{}]", NettyUtil.getClientId(channelHandlerContext),
                        result.cause());
                channelHandlerContext.disconnect();
                return;
            }
            if (resultHandlerRunnable != null) {
                resultHandlerRunnable.run();
            }
        };
        execute(blockingCodeHandler, asyncResultHandler, DEAL_MESSAGE_WORKER_EXECUTOR);
    }

    public static <T> void execute(Handler<Promise<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler, WorkerExecutor workerExecutor) {
        if (resultHandler == null) {
            resultHandler = result -> {
                if (result.cause() != null) {
                    log.error("worker thread execute error", result.cause());
                }
            };
        }
        workerExecutor.executeBlocking(blockingCodeHandler, false, resultHandler);
    }
}
