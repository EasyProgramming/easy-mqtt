package com.ep.mqtt.server.util;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/8/15 18:06
 */
@Slf4j
public class RedisTemplateUtil {

    public static <V> Set<String> scan(RedisTemplate<String, V> redisTemplate, String pattern, Integer size) {
        return redisTemplate.execute((RedisCallback<Set<String>>)connection -> {
            Set<String> matchKeySet = new HashSet<>();
            try (
                Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(size).build())) {
                while (cursor.hasNext()) {
                    matchKeySet.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return matchKeySet;
        });
    }

    public static <V> void scan(RedisTemplate<String, V> redisTemplate, String pattern, Integer size,
        Consumer<String> consumer) {
        redisTemplate.execute((RedisCallback<Set<String>>)connection -> {
            try (
                Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(size).build())) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next(), StandardCharsets.UTF_8);
                    log.debug("scan deal key {}", key);
                    consumer.accept(key);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });

    }

    public static <V> void hScan(RedisTemplate<String, V> redisTemplate, String hashKey, String pattern, Integer size,
        Consumer<Map.Entry<byte[], byte[]>> consumer) {
        redisTemplate.execute((RedisCallback<Set<String>>)connection -> {
            try (Cursor<Map.Entry<byte[], byte[]>> cursor = connection.hashCommands().hScan(hashKey.getBytes(),
                ScanOptions.scanOptions().match(pattern).count(size).build())) {
                while (cursor.hasNext()) {
                    Map.Entry<byte[], byte[]> entry = cursor.next();
                    log.debug("hash scan deal key {}", new String(entry.getKey(), StandardCharsets.UTF_8));
                    consumer.accept(entry);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public static <V> void sScan(RedisTemplate<String, V> redisTemplate, String setKey, String pattern, Integer size,
        Consumer<byte[]> consumer) {
        redisTemplate.execute((RedisCallback<Set<String>>)connection -> {
            try (Cursor<byte[]> cursor = connection.setCommands().sScan(setKey.getBytes(),
                ScanOptions.scanOptions().match(pattern).count(size).build())) {
                while (cursor.hasNext()) {
                    byte[] entry = cursor.next();
                    log.debug("set scan deal key {}", new String(entry, StandardCharsets.UTF_8));
                    consumer.accept(entry);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }
}
