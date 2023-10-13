package com.ep.mqtt.server.util;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.api.sync.RedisSetCommands;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConverters;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author zbz
 * @date 2023/8/15 18:06
 */
@Slf4j
public class RedisTemplateUtil {

    public static StatefulRedisConnection<String, String> getStatefulRedisConnection(RedisConnection connection){
        if (connection.isQueueing() || connection.isPipelined()) {
            throw new UnsupportedOperationException("'SCAN' cannot be called in pipeline / transaction mode.");
        }
        Object nativeConnection = connection.getNativeConnection();
        if (!(nativeConnection instanceof StatefulRedisConnection)){
            throw new UnsupportedOperationException("only support lettuce");
        }
        return (StatefulRedisConnection<String, String>) nativeConnection;
    }

    public static Set<String> lettuceScan(RedisConnection connection, String pattern, Integer size, Consumer<String> consumer) {
        StatefulRedisConnection<String, String> statefulConnection = getStatefulRedisConnection(connection);
        RedisCommands<String, String> redisCommands = statefulConnection.sync();
        ScanCursor cursor = ScanCursor.INITIAL;
        KeyScanCursor<String> keyScanCursor;
        Set<String> allMatchKeySet = Sets.newHashSet();
        do {
            ScanArgs scanArgs = ScanArgs.Builder.limit(size).match(pattern);
            keyScanCursor = redisCommands.scan(cursor, scanArgs);
            cursor = ScanCursor.of(keyScanCursor.getCursor());
            List<String> matchKeyList = keyScanCursor.getKeys();
            if (CollectionUtils.isEmpty(matchKeyList)){
               break;
            }
            if (consumer != null){
                for (String matchKey : matchKeyList){
                    consumer.accept(matchKey);
                }
            }
            allMatchKeySet.addAll(matchKeyList);
        } while (!keyScanCursor.isFinished());
        return allMatchKeySet;
    }

    public static Map<String, String> lettuceHashScan(RedisConnection connection, String hashKey, String pattern, Integer size,
                                                      Consumer<Map.Entry<String, String>> consumer) {
        StatefulRedisConnection<String, String> statefulConnection = getStatefulRedisConnection(connection);
        RedisHashCommands<String, String> redisHashCommands = statefulConnection.sync();
        ScanCursor cursor = ScanCursor.INITIAL;
        MapScanCursor<String, String> mapScanCursor;
        Map<String, String> allMatchMap = Maps.newHashMap();
        do {
            ScanArgs scanArgs = ScanArgs.Builder.limit(size).match(pattern);
            mapScanCursor = redisHashCommands.hscan(hashKey, cursor, scanArgs);
            cursor = ScanCursor.of(mapScanCursor.getCursor());
            Map<String, String> matchMap = mapScanCursor.getMap();
            if (CollectionUtils.isEmpty(matchMap)){
                break;
            }
            if (consumer != null){
                for (Map.Entry<String, String> entry : matchMap.entrySet()){
                    consumer.accept(entry);
                }
            }
            allMatchMap.putAll(matchMap);
        } while (!mapScanCursor.isFinished());
        return allMatchMap;
    }


    public static Set<String> lettuceSetScan(RedisConnection connection, String pattern, String setKey, Integer size,Consumer<String> consumer) {
        StatefulRedisConnection<String, String> statefulConnection = getStatefulRedisConnection(connection);
        RedisSetCommands<String, String> redisSetCommands = statefulConnection.sync();
        ScanCursor cursor = ScanCursor.INITIAL;
        ValueScanCursor<String> valueScanCursor;
        Set<String> allMatchKeySet = Sets.newHashSet();
        do {
            ScanArgs scanArgs = ScanArgs.Builder.limit(size).match(pattern);
            valueScanCursor = redisSetCommands.sscan(setKey, cursor, scanArgs);
            cursor = ScanCursor.of(valueScanCursor.getCursor());
            List<String> matchKeyList = valueScanCursor.getValues();
            if (CollectionUtils.isEmpty(matchKeyList)){
                break;
            }
            if (consumer != null){
                for (String matchKey : matchKeyList){
                    consumer.accept(matchKey);
                }
            }
            allMatchKeySet.addAll(matchKeyList);
        } while (!valueScanCursor.isFinished());
        return allMatchKeySet;
    }


    public static <V> Set<String> scan(RedisTemplate<String, V> redisTemplate, String pattern, Integer size) {
        return redisTemplate.execute((RedisCallback<Set<String>>)connection -> lettuceScan(connection, pattern, size, null));
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
