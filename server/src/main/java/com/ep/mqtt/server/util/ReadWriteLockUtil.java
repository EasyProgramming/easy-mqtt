package com.ep.mqtt.server.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * @author zbz
 * @date 2025/3/5 11:53
 */
public class ReadWriteLockUtil {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final Lock readLock = rwLock.readLock();

    private final Lock writeLock = rwLock.writeLock();

    /**
     * 执行读操作任务
     * @param <T> 返回值类型
     * @param supplier 需要在读锁保护下执行的任务
     * @return 任务执行结果
     */
    public <T> T readLock(Supplier<T> supplier) {
        readLock.lock();
        try {
            return supplier.get();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 执行写操作任务
     * @param task 需要在写锁保护下执行的任务
     */
    public void writeLock(Runnable task) {
        writeLock.lock();
        try {
            task.run();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 执行写操作任务
     */
    public  <T> T writeLock(Supplier<T> supplier) {
        writeLock.lock();
        try {
            return supplier.get();
        } finally {
            writeLock.unlock();
        }
    }


}
