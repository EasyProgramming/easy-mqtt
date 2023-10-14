package com.ep.mqtt.server.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.lettuce.core.*;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/8/15 18:06
 */
@Slf4j
public class RedisTemplateUtil {

    public static RedisClusterAsyncCommands<byte[], byte[]> getStatefulRedisConnection(RedisConnection connection) {
        if (connection.isQueueing() || connection.isPipelined()) {
            throw new UnsupportedOperationException("'SCAN' cannot be called in pipeline / transaction mode.");
        }
        Object nativeConnection = connection.getNativeConnection();
        if (nativeConnection instanceof RedisClusterAsyncCommands) {
            return (RedisClusterAsyncCommands<byte[], byte[]>)nativeConnection;
        }
        throw new UnsupportedOperationException("only support lettuce commands");

    }

    public static Set<String> lettuceScan(RedisConnection connection, String pattern, Integer size,
        Consumer<String> consumer) {
        RedisClusterAsyncCommands<byte[], byte[]> redisClusterAsyncCommands = getStatefulRedisConnection(connection);
        KeyScanCursor<byte[]> keyScanCursor = null;
        Set<String> allMatchKeySet = Sets.newHashSet();
        do {
            ScanArgs scanArgs = ScanArgs.Builder.limit(size).match(pattern);
            RedisFuture<KeyScanCursor<byte[]>> keyScanCursorRedisFuture;
            if (keyScanCursor == null) {
                keyScanCursorRedisFuture = redisClusterAsyncCommands.scan(scanArgs);
            } else {
                keyScanCursorRedisFuture = redisClusterAsyncCommands.scan(keyScanCursor, scanArgs);
            }
            try {
                keyScanCursor = keyScanCursorRedisFuture.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("lettuce scan error", e);
                break;
            }
            List<String> matchKeyList = convert(keyScanCursor.getKeys());
            if (CollectionUtils.isEmpty(matchKeyList)) {
                continue;
            }
            if (consumer != null) {
                for (String matchKey : matchKeyList) {
                    consumer.accept(matchKey);
                }
            }
            allMatchKeySet.addAll(matchKeyList);
        } while (!keyScanCursor.isFinished());
        return allMatchKeySet;
    }

    public static Map<String, String> lettuceHashScan(RedisConnection connection, String hashKey, String pattern,
        Integer size, Consumer<Map.Entry<String, String>> consumer) {
        RedisClusterAsyncCommands<byte[], byte[]> redisClusterAsyncCommands = getStatefulRedisConnection(connection);
        MapScanCursor<byte[], byte[]> mapScanCursor = null;
        Map<String, String> allMatchMap = Maps.newHashMap();
        do {
            ScanArgs scanArgs = ScanArgs.Builder.limit(size).match(pattern);
            RedisFuture<MapScanCursor<byte[], byte[]>> mapScanCursorRedisFuture;
            if (mapScanCursor == null) {
                mapScanCursorRedisFuture = redisClusterAsyncCommands.hscan(hashKey.getBytes(), scanArgs);
            } else {
                mapScanCursorRedisFuture = redisClusterAsyncCommands.hscan(hashKey.getBytes(), mapScanCursor, scanArgs);
            }
            try {
                mapScanCursor = mapScanCursorRedisFuture.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("lettuce hash scan error", e);
                break;
            }
            Map<byte[], byte[]> matchBytesMap = mapScanCursor.getMap();
            if (CollectionUtils.isEmpty(matchBytesMap)) {
                continue;
            }
            Map<String, String> matchMap = Maps.newHashMap();
            for (Map.Entry<byte[], byte[]> entry : matchBytesMap.entrySet()) {
                matchMap.put(new String(entry.getKey(), StandardCharsets.UTF_8),
                    new String(entry.getValue(), StandardCharsets.UTF_8));
            }
            if (consumer != null) {
                for (Map.Entry<String, String> entry : matchMap.entrySet()) {
                    consumer.accept(entry);
                }
            }
            allMatchMap.putAll(matchMap);
        } while (!mapScanCursor.isFinished());
        return allMatchMap;
    }

    public static Set<String> lettuceSetScan(RedisConnection connection, String pattern, String setKey, Integer size,
        Consumer<String> consumer) {
        RedisClusterAsyncCommands<byte[], byte[]> redisClusterAsyncCommands = getStatefulRedisConnection(connection);
        ValueScanCursor<byte[]> valueScanCursor = null;
        Set<String> allMatchValueSet = Sets.newHashSet();
        do {
            ScanArgs scanArgs = ScanArgs.Builder.limit(size).match(pattern);
            RedisFuture<ValueScanCursor<byte[]>> valueScanCursorRedisFuture;
            if (valueScanCursor == null) {
                valueScanCursorRedisFuture = redisClusterAsyncCommands.sscan(setKey.getBytes(), scanArgs);
            } else {
                valueScanCursorRedisFuture =
                    redisClusterAsyncCommands.sscan(setKey.getBytes(), valueScanCursor, scanArgs);
            }
            try {
                valueScanCursor = valueScanCursorRedisFuture.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("lettuce set scan error", e);
                break;
            }
            List<String> matchValueList = convert(valueScanCursor.getValues());
            if (CollectionUtils.isEmpty(matchValueList)) {
                continue;
            }
            if (consumer != null) {
                for (String matchKey : matchValueList) {
                    consumer.accept(matchKey);
                }
            }
            allMatchValueSet.addAll(matchValueList);
        } while (!valueScanCursor.isFinished());
        return allMatchValueSet;
    }

    private static List<String> convert(List<byte[]> bytesList) {
        List<String> stringList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(bytesList)) {
            return stringList;
        }
        for (byte[] bytes : bytesList) {
            stringList.add(new String(bytes, StandardCharsets.UTF_8));
        }
        return stringList;
    }

    public static <V> Set<String> scan(RedisTemplate<String, V> redisTemplate, String pattern, Integer size) {
        return redisTemplate
            .execute((RedisCallback<Set<String>>)connection -> lettuceScan(connection, pattern, size, null));
    }

    public static <V> void scan(RedisTemplate<String, V> redisTemplate, String pattern, Integer size,
        Consumer<String> consumer) {
        redisTemplate.execute((RedisCallback<Set<String>>)connection -> {
            lettuceScan(connection, pattern, size, consumer);
            return null;
        });
    }

    public static <V> void hScan(RedisTemplate<String, V> redisTemplate, String hashKey, String pattern, Integer size,
        Consumer<Map.Entry<String, String>> consumer) {
        redisTemplate.execute((RedisCallback<Set<String>>)connection -> {
            lettuceHashScan(connection, hashKey, pattern, size, consumer);
            return null;
        });
    }

    public static <V> void sScan(RedisTemplate<String, V> redisTemplate, String setKey, String pattern, Integer size,
        Consumer<String> consumer) {
        redisTemplate.execute((RedisCallback<Set<String>>)connection -> {
            lettuceSetScan(connection, setKey, pattern, size, consumer);
            return null;
        });
    }
}
