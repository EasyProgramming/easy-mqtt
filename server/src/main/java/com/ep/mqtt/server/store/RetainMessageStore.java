package com.ep.mqtt.server.store;

import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.metadata.Qos;
import com.ep.mqtt.server.util.JsonUtil;
import com.ep.mqtt.server.util.ReadWriteLockUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author : zbz
 * @date : 2025/3/3
 */
public class RetainMessageStore {

    private final static ReadWriteLockUtil LOCK = new ReadWriteLockUtil();

    private final static Map<String, RetainMessage> RETAIN_MESSAGE_MAP = Maps.newConcurrentMap();

    private final static TopicTree TOPIC_TREE = new TopicTree();

    public static List<RetainMessage> matchRetainMessage(String topicFilter) {
       return LOCK.readLock(()->{
            Set<String> topicSet = TOPIC_TREE.match(topicFilter);

            List<RetainMessage> retainMessageList = Lists.newArrayList();
            for (String topic : topicSet){
                RetainMessage retainMessage = RETAIN_MESSAGE_MAP.get(topic);
                if (retainMessage == null){
                    continue;
                }

                retainMessageList.add(retainMessage);
            }

            return retainMessageList;
        });

    }

    public static void add(RetainMessage retainMessage) {
        LOCK.writeLock(()->{
            TOPIC_TREE.insert(retainMessage.getTopic());
            RETAIN_MESSAGE_MAP.put(retainMessage.getTopic(), retainMessage);
        });
    }

    public static void remove(String topic) {
        LOCK.writeLock(()->{
            TOPIC_TREE.delete(topic);
            RETAIN_MESSAGE_MAP.remove(topic);
        });
    }

    public static Map<String, RetainMessage> getRetainMessageMap() {
        return RETAIN_MESSAGE_MAP;
    }

    @Data
    public static class RetainMessage {

        private String topic;

        private String payload;

        private Qos receiveQos;

        private Integer receivePacketId;

        private String fromClientId;

    }

    @Data
    public static class TopicTree {
        private final TreeNode root = new TreeNode(); // 根节点

        public void insert(String topic) {
            String[] parts = split(topic);
            TreeNode current = root;
            for (String part : parts) {
                current = current.children.computeIfAbsent(part, k -> new TreeNode());
            }
            current.topic = topic; // 终端节点存储完整主题
        }

        /**
         * 删除主题（若存在）
         *
         * @return 是否成功删除
         */
        public boolean delete(String topic) {
            String[] parts = split(topic);
            return delete(root, parts, 0);
        }

        /**
         * 递归删除节点（后序遍历清理空节点）
         */
        private boolean delete(TreeNode current, String[] parts, int depth) {
            if (depth == parts.length) {
                if (current.topic == null) {
                    return false;
                }

                current.topic = null;
                return true;
            }

            String part = parts[depth];
            TreeNode child = current.children.get(part);
            if (child == null) {
                return false;
            }

            boolean deleted = delete(child, parts, depth + 1);
            if (deleted && child.topic == null && child.children.isEmpty()) {
                // 清理空节点
                current.children.remove(part);
            }
            return deleted;
        }

        /**
         * 匹配带通配符的 Topic Filter
         *
         * @return 所有匹配的具体主题集合
         */
        public Set<String> match(String topicFilter) {
            String[] parts = split(topicFilter);
            Set<String> result = new HashSet<>();
            match(root, parts, 0, result);
            return result;
        }

        // 递归匹配核心逻辑（深度优先遍历）
        private void match(TreeNode node, String[] filterParts, int depth, Set<String> result) {
            if (depth == filterParts.length) {
                if (node.topic != null) {
                    result.add(node.topic); // 完全匹配
                }
                return;
            }

            String part = filterParts[depth];
            if (part.equals(Constant.TOPIC_WILDCARDS_ONE)) {
                for (TreeNode child : node.children.values()) {
                    match(child, filterParts, depth + 1, result);
                }
            } else if (part.equals(Constant.TOPIC_WILDCARDS_MORE)) {
                collectSubtree(node, result);
            } else {
                TreeNode child = node.children.get(part);
                if (child != null) {
                    match(child, filterParts, depth + 1, result);
                }
            }
        }

        /**
         * 收集子树所有终端节点（用于 # 通配符）
         */
        private void collectSubtree(TreeNode node, Set<String> result) {
            if (node.topic != null) {
                result.add(node.topic);
            }

            for (TreeNode child : node.children.values()) {
                collectSubtree(child, result);
            }
        }

        private String[] split(String text) {
            if (text == null || text.isEmpty()) {
                throw new IllegalArgumentException("text cannot be empty");
            }

            return StringUtils.split(text, Constant.FORWARD_SLASH);
        }

        @Data
        private static class TreeNode {
            /**
             * 子节点
             */
            private Map<String, TreeNode> children = new HashMap<>();

            /**
             * 存储完整主题（仅终端节点有效）
             */
            private String topic;
        }

        // 测试用例
        public static void main(String[] args) {
            TopicTree trie = new TopicTree();

            // 插入测试数据
            trie.insert("sensor");
            trie.insert("sensor/room1/temp");
            trie.insert("sensor/room2/temp");
            trie.insert("sensor/room1/humidity");
            trie.insert("home/living_room/light");

            // 匹配测试
            System.out.println("Test 1: sensor/+/temp → " + trie.match("sensor/+/temp")); // [sensor/room1/temp, sensor/room2/temp]

            System.out.println("Test 2: sensor/# → " + trie.match("sensor/#")); // [sensor, sensor/room1/temp, sensor/room2/temp,
            // sensor/room1/humidity]

            System.out.println("Test 3: home/# → " + trie.match("home/#")); // [home/living_room/light]

            // 删除测试
            trie.delete("sensor");
            System.out.println(JsonUtil.obj2String(trie.getRoot()));
            System.out.println("After delete - Test 1: sensor/+/temp → " + trie.match("sensor/+/temp")); // [sensor/room2/temp]
        }
    }
}
