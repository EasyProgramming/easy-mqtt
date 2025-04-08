package com.ep.mqtt.server.queue;

import com.ep.mqtt.server.db.dao.SendMessageDao;
import com.ep.mqtt.server.db.dto.SendMessageDto;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.metadata.RpcCommand;
import com.ep.mqtt.server.rpc.EasyMqttRpcClient;
import com.ep.mqtt.server.rpc.transfer.SendMessage;
import com.ep.mqtt.server.util.ModelUtil;
import com.ep.mqtt.server.util.ReadWriteLockUtil;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author zbz
 * @date 2025/4/3 11:29
 */
@Slf4j
public class InsertSendMessageQueue {

    public static int QUEUE_SIZE = 50000;

    public static int BATCH_INSERT_SIZE = 2000;

    private final static ReadWriteLockUtil LOCK = new ReadWriteLockUtil();

    /**
     * 自动插入的线程池
     */
    private static final ScheduledThreadPoolExecutor AUTO_INSERT_THREAD_POOL =
        new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setNameFormat("auto-insert-send-message-%s").build());

    public static ThreadPoolExecutor BATCH_INSERT_SEND_MESSAGE_THREAD_POOL = new ThreadPoolExecutor(Constant.PROCESSOR_NUM, Constant.PROCESSOR_NUM,
        60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat("batch-insert-send-message-%s").build());

    public static ArrayBlockingQueue<SendMessageDto> QUEUE = new ArrayBlockingQueue<>(QUEUE_SIZE);

    private Thread consumerThread;

    private final SendMessageDao sendMessageDao;

    public InsertSendMessageQueue(SendMessageDao sendMessageDao) {
        this.sendMessageDao = sendMessageDao;
    }

    @SuppressWarnings("AlibabaAvoidManuallyCreateThread")
    public void start() {
        AUTO_INSERT_THREAD_POOL.scheduleWithFixedDelay(new AutoInsertSendMessageRunnable(this.sendMessageDao), 60, 200, TimeUnit.MILLISECONDS);

        consumerThread = new Thread(() -> {
            while (true) {
                try {
                    int currentQueueSize = InsertSendMessageQueue.QUEUE.size();
                    if (InsertSendMessageQueue.QUEUE_SIZE * 0.8 < currentQueueSize){
                        log.warn("队列剩余容量不足，当前容量：[{}]", currentQueueSize);
                    }

                    SendMessageDto sendMessageDto = QUEUE.take();
                    if (sendMessageDto.getSendQos().equals(Qos.LEVEL_0)) {
                        send(sendMessageDto);

                        continue;
                    }

                    LOCK.writeLock(() -> {
                        DataManage.add(sendMessageDto);

                        if (DataManage.size() < BATCH_INSERT_SIZE) {
                            return;
                        }

                        doInsert(sendMessageDao);
                    });
                } catch (InterruptedException e) {
                    log.warn("插入发送消息队列被打断");
                    return;
                } catch (Throwable throwable) {
                    log.warn("插入发送消息队列异常", throwable);
                }
            }
        });

        consumerThread.start();
    }

    public void stop() {
        while (!QUEUE.isEmpty()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                log.warn("插入发送消息队列被打断", e);
            }
        }

        consumerThread.interrupt();

        AUTO_INSERT_THREAD_POOL.execute(new AutoInsertSendMessageRunnable(sendMessageDao));
        AUTO_INSERT_THREAD_POOL.shutdown();

        BATCH_INSERT_SEND_MESSAGE_THREAD_POOL.shutdown();
    }

    public static void add(List<SendMessageDto> sendMessageDtoList, Map<String, Integer> sendMessageIdMap){
        for (SendMessageDto sendMessageDto : sendMessageDtoList){
            if (!sendMessageDto.getSendQos().equals(Qos.LEVEL_0)){
                Integer sendPacketId = sendMessageIdMap.get(sendMessageDto.getToClientId());

                if (sendPacketId == null){
                    continue;
                }

                sendMessageDto.setSendPacketId(sendPacketId);
            }

            QUEUE.add(sendMessageDto);
        }
    }

    public static class AutoInsertSendMessageRunnable implements Runnable {

        private final static Long AUTO_INSERT_THRESHOLD = 200L;

        private final SendMessageDao sendMessageDao;

        public AutoInsertSendMessageRunnable(SendMessageDao sendMessageDao) {
            this.sendMessageDao = sendMessageDao;
        }

        @Override
        public void run() {
            try {
                if (System.currentTimeMillis() - DataManage.getLastEditTime() <= AUTO_INSERT_THRESHOLD) {
                    return;
                }

                LOCK.writeLock(() -> doInsert(sendMessageDao));
            } catch (Throwable e) {
                log.error("自动插入发送消息异常", e);
            }
        }
    }

    private static void doInsert(SendMessageDao sendMessageDao) {
        if (DataManage.size() <= 0) {
            return;
        }

        List<SendMessageDto> tempList = DataManage.get();
        BATCH_INSERT_SEND_MESSAGE_THREAD_POOL.submit(() -> {
            try {
                sendMessageDao.insert(tempList);

                for (SendMessageDto sendMessageDto : tempList) {
                    send(sendMessageDto);
                }
            } catch (Throwable e) {
                log.error("批量插入发送消息异常", e);
            }
        });

        DataManage.clear();
    }

    private static void send(SendMessageDto sendMessageDto) {
        SendMessage sendMessage = ModelUtil.buildSendMessage(sendMessageDto.getSendQos(), sendMessageDto.getTopic(), sendMessageDto.getSendPacketId(),
            sendMessageDto.getToClientId(), sendMessageDto.getPayload(), false, sendMessageDto.getIsRetain().getBoolean());

        EasyMqttRpcClient.broadcast(RpcCommand.SEND_MESSAGE, sendMessage);
    }

    @Data
    public static class DataManage {

        private static Long LAST_EDIT_TIME = 0L;

        private static ReadWriteLockUtil LOCK = new ReadWriteLockUtil();

        private static List<SendMessageDto> DATA_LIST = Lists.newArrayList();

        public static void add(SendMessageDto sendMessageDto) {
            LOCK.writeLock(() -> {
                DATA_LIST.add(sendMessageDto);

                LAST_EDIT_TIME = System.currentTimeMillis();
            });
        }

        public static void clear() {
            LOCK.writeLock(() -> {
                DATA_LIST.clear();

                LAST_EDIT_TIME = System.currentTimeMillis();
            });
        }

        public static List<SendMessageDto> get() {
            return LOCK.readLock(() -> Lists.newArrayList(DATA_LIST));
        }

        public static int size() {
            return LOCK.readLock(() -> DATA_LIST.size());
        }

        public static Long getLastEditTime() {
            return LOCK.readLock(() -> LAST_EDIT_TIME);
        }

    }
}
