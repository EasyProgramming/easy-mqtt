package com.ep.mqtt.server.util;

import io.vertx.core.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author zbz
 * @date 2023/7/25 17:09
 */
@Slf4j
public class WorkerThreadPool {

    public static Vertx VERTX;

    private static WorkerExecutor WORKER_EXECUTOR;

    private static final Long MAX_EXECUTE_TIME = 20L;

    private static final TimeUnit MAX_EXECUTE_TIME_UNIT = TimeUnit.MILLISECONDS;

    public static void init(Integer workerThreadPoolSize){
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setWarningExceptionTime(MAX_EXECUTE_TIME * 2);
        vertxOptions.setWarningExceptionTimeUnit(MAX_EXECUTE_TIME_UNIT);
        VERTX = Vertx.vertx(vertxOptions);
        WORKER_EXECUTOR =
                VERTX.createSharedWorkerExecutor("worker-thread-pool", workerThreadPoolSize, MAX_EXECUTE_TIME, MAX_EXECUTE_TIME_UNIT);
    }

    public static <T> void execute(Handler<Promise<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler) {
        if (resultHandler == null) {
            resultHandler = result -> {
                if (result.cause() != null) {
                    log.error("worker thread execute error", result.cause());
                }
            };
        }
        WORKER_EXECUTOR.executeBlocking(blockingCodeHandler, false, resultHandler);
    }

}
