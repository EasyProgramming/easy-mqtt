package com.ep.mqtt.server.util;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/8/16 13:50
 */
@Slf4j
public class TopicUtilTest {

    @Test
    public void testValidateTopicFilter_1() {
        TopicUtil.validateTopicFilter("aaa/");
    }

}
