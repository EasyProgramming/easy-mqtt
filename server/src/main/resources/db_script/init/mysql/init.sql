-- ----------------------------
-- Table structure for async_job
-- ----------------------------
CREATE TABLE `async_job`  (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `business_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                              `business_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                              `last_start_time` bigint NULL DEFAULT NULL,
                              `last_end_time` bigint NULL DEFAULT NULL,
                              `last_execute_result` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                              `last_execute_result_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                              `expect_execute_time` bigint NOT NULL,
                              `execute_num` int NOT NULL,
                              `execute_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                              `extend_data` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                              `job_param` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                              PRIMARY KEY (`id`) USING BTREE,
                              UNIQUE INDEX `index_1`(`business_id`) USING BTREE,
                              INDEX `index_2`(`expect_execute_time`, `id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for client
-- ----------------------------
CREATE TABLE `client`  (
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `client_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                           `last_connect_time` bigint NOT NULL,
                           `create_time` bigint NOT NULL,
                           `is_clean_session` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                           `message_id_progress` bigint NOT NULL,
                           PRIMARY KEY (`id`) USING BTREE,
                           UNIQUE INDEX `index_1`(`client_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for client_subscribe
-- ----------------------------
CREATE TABLE `client_subscribe`  (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `client_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                     `topic_filter` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                     `subscribe_time` bigint NOT NULL,
                                     `qos` int NOT NULL,
                                     PRIMARY KEY (`id`) USING BTREE,
                                     INDEX `index_1`(`client_id`) USING BTREE,
  INDEX `index_2`(`topic_filter`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for meta_data
-- ----------------------------
CREATE TABLE `meta_data`  (
                              `key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                              `value` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                              `desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                              PRIMARY KEY (`key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for receive_qos2_message
-- ----------------------------
CREATE TABLE `receive_qos2_message`  (
                                         `id` bigint NOT NULL AUTO_INCREMENT,
                                         `receive_qos` int NOT NULL,
                                         `topic` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                         `receive_packet_id` int NOT NULL,
                                         `from_client_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                         `payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                         `receive_time` bigint NOT NULL,
                                         `is_retain` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                         PRIMARY KEY (`id`) USING BTREE,
                                         UNIQUE INDEX `index_1`(`from_client_id`, `receive_packet_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for retain_message
-- ----------------------------
CREATE TABLE `retain_message`  (
                                   `id` bigint NOT NULL AUTO_INCREMENT,
                                   `payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                   `receive_qos` int NOT NULL,
                                   `topic` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                   PRIMARY KEY (`id`) USING BTREE,
                                   INDEX `index_1`(`topic`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for send_message
-- ----------------------------
CREATE TABLE `send_message`  (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `receive_qos` int NOT NULL,
                                 `receive_packet_id` int NOT NULL,
                                 `from_client_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                 `send_qos` int NOT NULL,
                                 `topic` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                 `send_packet_id` int NOT NULL,
                                 `to_client_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                 `payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                 `is_receive_pub_rec` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                 `valid_time` bigint NOT NULL,
                                 `is_retain` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                 PRIMARY KEY (`id`) USING BTREE,
                                 INDEX `index_1`(`to_client_id`, `send_packet_id`, `send_qos`, `is_receive_pub_rec`) USING BTREE,
  INDEX `index_2`(`topic`) USING BTREE,
  INDEX `index_3`(`valid_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for topic_filter
-- ----------------------------
CREATE TABLE `topic_filter`  (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `topic_filter` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                 PRIMARY KEY (`id`) USING BTREE,
                                 UNIQUE INDEX `index_1`(`topic_filter`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;
