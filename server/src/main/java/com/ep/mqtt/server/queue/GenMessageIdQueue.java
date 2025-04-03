package com.ep.mqtt.server.queue;

import com.ep.mqtt.server.job.GenMessageIdParam;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author zbz
 * @date 2025/4/3 11:29
 */
@Slf4j
public class GenMessageIdQueue {

    public static ArrayBlockingQueue<GenMessageIdParam> QUEUE = new ArrayBlockingQueue<>(10000);

    @SuppressWarnings("AlibabaAvoidManuallyCreateThread")
    public void start(){
        new Thread(() -> {
            while (true) {
                try {
                    QUEUE.take();
                    /*
                        插入批量添加的数组

                        加入批量插入的线程池；

                        批量发送消息
                     */

                    /*
                        需要有个定时兜底提交的任务，当间隔>200ms时，没达到批量提交的阈值，强制提交
                     */

                }
                catch (InterruptedException e) {
                    return;
                }
                catch (Throwable throwable) {

                }
            }
        }).start();
    }

}
