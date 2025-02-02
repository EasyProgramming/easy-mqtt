-- ----------------------------
-- Table structure for async_job
-- ----------------------------
CREATE TABLE "async_job"
(
    "id"                       integer NOT NULL,
    "business_id"              text    NOT NULL,
    "business_type"            text    NOT NULL,
    "last_start_time"          integer,
    "last_end_time"            integer,
    "last_execute_result"      text,
    "last_execute_result_desc" text,
    "expect_execute_time"      integer NOT NULL,
    "execute_num"              integer NOT NULL,
    "execute_status"           text    NOT NULL,
    "extend_data"              text,
    "job_param"                text    NOT NULL,
    PRIMARY KEY ("id")
);


-- ----------------------------
-- Table structure for client
-- ----------------------------
CREATE TABLE "client"
(
    "id"                integer NOT NULL,
    "client_id"         text    NOT NULL,
    "last_connect_time" integer NOT NULL,
    "create_time"       integer NOT NULL,
    "is_clean_session"  text    NOT NULL,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for client_subscribe
-- ----------------------------
CREATE TABLE "client_subscribe"
(
    "id"             INTEGER NOT NULL,
    "client_id"      text    NOT NULL,
    "topic_filter"   text    NOT NULL,
    "subscribe_time" integer NOT NULL,
    "qos"            integer NOT NULL,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for meta_data
-- ----------------------------
CREATE TABLE "meta_data"
(
    "key"   text NOT NULL,
    "value" text NOT NULL,
    "desc"  text NOT NULL,
    PRIMARY KEY ("key")
);

-- ----------------------------
-- Table structure for receive_qos2_message
-- ----------------------------
CREATE TABLE "receive_qos2_message"
(
    "id"                integer NOT NULL,
    "receive_qos"       integer NOT NULL,
    "topic"             text    NOT NULL,
    "receive_packet_id" integer NOT NULL,
    "from_client_id"    text    NOT NULL,
    "payload"           text    NOT NULL,
    "is_receive_pubrel" text    NOT NULL,
    "receive_time"      integer NOT NULL,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for retain_message
-- ----------------------------
CREATE TABLE "retain_message"
(
    "id"          integer NOT NULL,
    "payload"     text    NOT NULL,
    "receive_qos" integer NOT NULL,
    "topic"       text    NOT NULL,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for send_message
-- ----------------------------
CREATE TABLE "send_message"
(
    "id"                 integer NOT NULL,
    "receive_qos"        integer NOT NULL,
    "receive_packet_id"  text    NOT NULL,
    "from_client_id"     text    NOT NULL,
    "send_qos"           integer NOT NULL,
    "topic"              text    NOT NULL,
    "send_packet_id"     text    NOT NULL,
    "to_client_id"       text    NOT NULL,
    "payload"            text    NOT NULL,
    "is_receive_puback"  text    NOT NULL,
    "is_receive_pubrec"  text    NOT NULL,
    "is_receive_pubcomp" text    NOT NULL,
    "valid_time"         integer NOT NULL,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for topic_filter
-- ----------------------------
CREATE TABLE "topic_filter"
(
    "id"           integer NOT NULL,
    "topic_filter" text    NOT NULL,
    PRIMARY KEY ("id")
);

-- ----------------------------
-- Indexes structure for table async_job
-- ----------------------------
CREATE UNIQUE INDEX "async_job_index_1"
    ON "async_job" (
                    "business_id" ASC
        );
CREATE INDEX "async_job_index_2"
    ON "async_job" (
                    "expect_execute_time" ASC,
                    "execute_status" ASC
        );

-- ----------------------------
-- Indexes structure for table client
-- ----------------------------
CREATE UNIQUE INDEX "client_index_1"
    ON "client" (
                 "client_id" ASC
        );

-- ----------------------------
-- Indexes structure for table client_subscribe
-- ----------------------------
CREATE INDEX "client_subscribe_index_1"
    ON "client_subscribe" (
                           "client_id" ASC
        );
CREATE INDEX "client_subscribe_index_2"
    ON "client_subscribe" (
                           "topic_filter" ASC
        );

-- ----------------------------
-- Indexes structure for table receive_qos2_message
-- ----------------------------
CREATE UNIQUE INDEX "receive_qos2_message_index_1"
    ON "receive_qos2_message" (
                               "from_client_id" ASC,
                               "receive_packet_id" ASC
        );

-- ----------------------------
-- Indexes structure for table retain_message
-- ----------------------------
CREATE INDEX "retain_message_index_1"
    ON "retain_message" (
                         "topic" ASC
        );

-- ----------------------------
-- Indexes structure for table send_message
-- ----------------------------
CREATE INDEX "send_message_index_1"
    ON "send_message" (
                       "to_client_id" ASC,
                       "send_packet_id" ASC
        );
CREATE INDEX "send_message_index_2"
    ON "send_message" (
                       "topic" ASC
        );
CREATE INDEX "send_message_index_3"
    ON "send_message" (
                       "valid_time" ASC
        );

-- ----------------------------
-- Indexes structure for table topic_filter
-- ----------------------------
CREATE UNIQUE INDEX "topic_filter_index_1"
    ON "topic_filter" (
                       "topic_filter" ASC
        );