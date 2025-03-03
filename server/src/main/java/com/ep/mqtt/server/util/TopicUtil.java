package com.ep.mqtt.server.util;

/**
 * @author zbz
 * @date 2023/8/12 17:05
 */
public class TopicUtil {

    public static final char TOPIC_WILDCARDS_ONE = '+';

    public static final char TOPIC_WILDCARDS_MORE = '#';

    public static final char TOPIC_SPLIT_FLAG = '/';

    /**
     * 校验 topicFilter
     *
     * @param topicFilter
     *            topicFilter
     */
    public static void validateTopicFilter(String topicFilter) throws IllegalArgumentException {
        if (topicFilter == null || topicFilter.isEmpty()) {
            throw new IllegalArgumentException("topic filter is blank:" + topicFilter);
        }
        char[] topicFilterChars = topicFilter.toCharArray();
        int topicFilterLength = topicFilterChars.length;
        int topicFilterIdxEnd = topicFilterLength - 1;
        char ch;
        for (int i = 0; i < topicFilterLength; i++) {
            ch = topicFilterChars[i];
            if (Character.isWhitespace(ch)) {
                throw new IllegalArgumentException("topic filter has white space:" + topicFilter);
            } else if (ch == TOPIC_WILDCARDS_MORE) {
                // 校验: # 通配符只能在最后一位
                if (i < topicFilterIdxEnd) {
                    throw new IllegalArgumentException("topic filter illegal:" + topicFilter);
                }
            } else if (ch == TOPIC_WILDCARDS_ONE) {
                // 校验: 单独 + 是允许的，判断 + 号前一位是否为 /
                if (i > 0 && topicFilterChars[i - 1] != TOPIC_SPLIT_FLAG) {
                    throw new IllegalArgumentException("topic filter illegal:" + topicFilter);
                }
            }
        }
    }

}
