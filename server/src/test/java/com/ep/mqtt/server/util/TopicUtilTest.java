package com.ep.mqtt.server.util;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2023/8/16 13:50
 */
@Slf4j
public class TopicUtilTest {

    @Test
    public void testConvertTopicFilterToRegex_1() {
        String topicFilter = "/+";
        Set<String> topicSet = Sets.newHashSet("/finance", "a/b/e/e/c/d", "c/ww/e/", "e/ww/e/c/c");
        testConvertTopicFilterToRegex(topicFilter, topicSet);
    }

    @Test
    public void testConvertTopicFilterToRegex_2() {
        String topicFilter = "+/+/+/+/c/#";
        Set<String> topicSet = Sets.newHashSet("/finance", "a/b/e/e/c/d", "c/ww/e/", "e/ww/e/c/c");
        testConvertTopicFilterToRegex(topicFilter, topicSet);
    }

    private void testConvertTopicFilterToRegex(String topicFilter, Set<String> topicSet) {
        String regex = TopicUtil.convertTopicFilterToRegex(topicFilter);
        log.info("regex: {}", regex);
        for (String topic : topicSet) {
            if (topic.matches(regex)) {
                log.info("match: {}", topic);
            }
        }
    }

    @Test
    public void testValidateTopicFilter_1() {
        TopicUtil.validateTopicFilter("aaa/");
    }

}
