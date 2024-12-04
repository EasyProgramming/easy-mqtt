package com.ep.mqtt.server.metadata;

/**
 * @author zbz
 * @date 2023/8/17 14:25
 */
public class Constant {

    public final static String STORE_KEY_SPLIT = "/";

    public final static String TOPIC_FILTER_KEY_PREFIX = "topic_filter";

    public final static String PROJECT_BASE_DIR = System.getenv("BASE_DIR") == null ? "" : System.getenv("BASE_DIR");

}

