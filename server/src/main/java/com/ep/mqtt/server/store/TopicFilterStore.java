package com.ep.mqtt.server.store;

import com.ep.mqtt.server.util.JsonUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author zbz
 * @date 2023/8/17 15:31
 */
@Slf4j
public class TopicFilterStore {

    private static final String LOCK_KEY = "TopicFilterStore";

    private static final Set<String> TOPIC_FILTER_SET = new HashSet<>();

    private static final TopicFilterTree TOPIC_FILTER_TREE = new TopicFilterTree();

    public static List<String> matchTopicFilter(String topicName) {
        synchronized (LOCK_KEY){
            return TOPIC_FILTER_TREE.getMatchingFilters(topicName);
        }
    }

    public static void add(String topicFilter) {
        synchronized (LOCK_KEY){
            TOPIC_FILTER_TREE.insert(topicFilter);
            TOPIC_FILTER_SET.add(topicFilter);
        }
    }

    public static void remove(String topicFilter) {
        synchronized (LOCK_KEY){
            TOPIC_FILTER_TREE.delete(topicFilter);
            TOPIC_FILTER_SET.remove(topicFilter);
        }
    }

    public static Set<String> getTopicFilterSet() {
        return TOPIC_FILTER_SET;
    }

    public static class TopicFilterTree {
        /**
         * 前缀树的根节点
         */
        private final TopicNode root;

        public TopicFilterTree() {
            // 初始化根节点
            root = new TopicNode();
        }

        /**
         * 插入一个 Topic Filter
         * @param filter 要插入的 Topic Filter，使用 `/` 分隔的字符串
         */
        public void insert(String filter) {
            // 从根节点开始
            TopicNode current = root;
            for (String part : StringUtils.splitByWholeSeparatorPreserveAllTokens(filter, "/")) {
                // 如果当前层级不存在，创建新节点
                current.children.putIfAbsent(part, new TopicNode());

                // 移动到下一个节点
                current = current.children.get(part);
            }

            // 标记为完整的过滤器
            current.isEndOfFilter = true;
        }

        /**
         * 获取与主题匹配的所有 Topic Filters
         * @param topic 要匹配的主题
         * @return 匹配的所有过滤器列表
         */
        public List<String> getMatchingFilters(String topic) {
            // 保存匹配的过滤器
            List<String> matchingFilters = new ArrayList<>();

            // 开始递归匹配
            collectMatches(root,  StringUtils.splitByWholeSeparatorPreserveAllTokens(topic, "/"), 0, null, matchingFilters);
            return matchingFilters;
        }

        /**
         * 递归收集匹配的过滤器
         * @param current 当前节点
         * @param levels 按 `/` 分隔的主题层级数组
         * @param index 当前层级的索引
         * @param path 当前路径（构建的过滤器字符串）
         * @param matches 保存匹配结果的列表
         */
        private void collectMatches(TopicNode current, String[] levels, int index, String path, List<String> matches) {
            // 如果当前节点为空，停止递归
            if (current == null) {
                return;
            }

            // 如果当前节点是完整过滤器，添加到结果列表
            if (current.isEndOfFilter) {
                matches.add(path);
            }

            // 如果已遍历完主题层级，检查是否有 `#` 节点匹配后续，针对的filter=a/a/#,topic=a/a；filter=a/#,topic=a
            if (index == levels.length) {
                // `#` 匹配后续所有层级
                if (current.children.containsKey("#")) {
                    matches.add(path + "/#");
                }
                return;
            }

            // 获取当前层级
            String level = levels[index];

            // 精确匹配当前层级
            if (current.children.containsKey(level)) {
                collectMatches(current.children.get(level), levels, index + 1,
                        path == null ? level : path + "/" + level, matches);
            }

            // 匹配单层通配符 `+`
            if (current.children.containsKey("+")) {
                collectMatches(current.children.get("+"), levels, index + 1,
                        path == null ? "+" : path + "/+", matches);
            }

            // 匹配多层通配符 `#`
            if (current.children.containsKey("#")) {
                matches.add(path == null ? "#" : path + "/#");
            }
        }

        /**
         * 删除一个 Topic Filter
         * @param filter 要删除的过滤器字符串
         */
        public void delete(String filter) {
            deleteRecursive(root,  StringUtils.splitByWholeSeparatorPreserveAllTokens(filter, "/"), 0); // 从根节点开始递归删除
        }

        /**
         * 递归删除过滤器
         * @param current 当前节点
         * @param parts 按 `/` 分隔的过滤器层级数组
         * @param index 当前层级索引
         * @return 如果可以删除当前节点，返回 true；否则返回 false
         */
        private boolean deleteRecursive(TopicNode current, String[] parts, int index) {
            // 如果已遍历完过滤器层级
            if (index == parts.length) {
                // 如果当前节点不是完整过滤器，过滤器不存在
                if (!current.isEndOfFilter) {
                    return false;
                }

                // 取消结束标记
                current.isEndOfFilter = false;

                // 如果没有子节点，可以删除当前节点
                return current.children.isEmpty();
            }

            // 获取当前层级
            String part = parts[index];
            // 获取子节点
            TopicNode child = current.children.get(part);
            // 如果子节点不存在，过滤器不存在
            if (child == null) {
                return false;
            }

            boolean shouldDeleteChild = deleteRecursive(child, parts, index + 1); // 递归删除子节点

            // 如果子节点可以删除
            if (shouldDeleteChild) {
                // 删除子节点
                current.children.remove(part);
                // 当前节点是否可以删除
                return current.children.isEmpty() && !current.isEndOfFilter;
            }

            // 当前节点不能删除
            return false;
        }

        @Data
        public static class TopicNode {
            /**
             * 保存当前节点的子节点映射
             */
            Map<String, TopicNode> children;

            /**
             * 是否是完整的 Topic Filter 的结束节点
             */
            boolean isEndOfFilter;

            public TopicNode() {
                // 初始化子节点映射
                children = new HashMap<>();
                // 初始化为非结束节点
                isEndOfFilter = false;
            }
        }

        public static void main(String[] args) {
            TopicFilterTree trie = new TopicFilterTree();

            // 插入 Topic Filter
            trie.insert("device/");
            trie.insert("device/#");
            trie.insert("sensor/+/temperature");
            trie.insert("sensor/#");
            trie.insert("/");
            trie.insert("/#");
            trie.insert("//#");
            trie.insert("///#");

            System.out.println(JsonUtil.obj2String(trie.root));

            // 测试匹配逻辑
            System.out.println(trie.getMatchingFilters("device/"));         // [device/, device/#]
            System.out.println(trie.getMatchingFilters("device"));          // [device/#]
            System.out.println(trie.getMatchingFilters("device/data"));     // [device/#]
            System.out.println(trie.getMatchingFilters("sensor/1/temperature")); // [sensor/+/temperature, sensor/#]
            System.out.println(trie.getMatchingFilters("sensor/1"));        // [sensor/#]
            System.out.println(trie.getMatchingFilters("sensor/"));         // [sensor/#]
            System.out.println(trie.getMatchingFilters("/"));         // [/, /#, //#]

            trie.delete("/");
            System.out.println(JsonUtil.obj2String(trie.root));
        }

    }

}
