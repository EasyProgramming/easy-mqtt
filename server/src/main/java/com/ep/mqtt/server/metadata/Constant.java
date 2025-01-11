package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2023/8/17 14:25
 */
public class Constant {

    public final static String STORE_KEY_SPLIT = "/";

    public final static String TOPIC_FILTER_KEY_PREFIX = "topic_filter";

    public final static String PROJECT_BASE_DIR = System.getenv("BASE_DIR") == null ? "" : System.getenv("BASE_DIR");

    public final static String CONFIG_FILE_PATH = System.getenv("CONFIG_FILE") == null ? "" : System.getenv("CONFIG_FILE");

    /**
     * cpu核心数
     */
    public static final Integer PROCESSOR_NUM = Runtime.getRuntime().availableProcessors();
}

