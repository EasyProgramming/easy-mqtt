package com.ep.mqtt.server.metadata;

/**
 * @author : zbz
 * @date : 2023/9/18
 */
public class LuaScript {

    // @formatter:off

    /**
     * 保存消息
     */
    public static String SAVE_MESSAGE = "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
                                        "    redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])\n" +
                                        "    return 1\n" +
                                        "end";

    /**
     * 产生消息id
     */
    public static String GEN_MESSAGE_ID = "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
                                          "    return redis.call('INCRBY', KEYS[1], 1)\n" +
                                          "end";

    /**
     * 保存REC消息
     */
    public static String SAVE_REC_MESSAGE = "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
                                            "    redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])\n" +
                                            "end";

    /**
     * 保存REC消息
     */
    public static String SAVE_REL_MESSAGE = "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
                                            "    redis.call('SADD', KEYS[1], ARGV[1])\n" +
                                            "end";


    /**
     * 保存topic filter版本数据
     */
    public static String SAVE_TOPIC_FILTER_VERSION_DATA = "local increment = redis.call('INCR', KEYS[1])\n" +
                                                          "local new_key = tostring(increment)\n" +
                                                          "redis.call('SET', new_key, ARGV[1])";

    /**
     * 保存topic filter版本数据
     */
    public static String SAVE_TOPIC_VERSION_DATA = "local increment = redis.call('INCR', KEYS[1])\n" +
                                                    "local new_key = tostring(increment)\n" +
                                                    "redis.call('SET', new_key, ARGV[1])";

    // @formatter:on

}
