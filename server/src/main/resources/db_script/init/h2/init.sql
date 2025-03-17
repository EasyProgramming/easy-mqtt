-- ----------------------------
-- Table structure for async_job
-- ----------------------------
create table ASYNC_JOB
(
    ID                       BIGINT  not null,
    BUSINESS_ID              VARCHAR not null,
    BUSINESS_TYPE            VARCHAR not null,
    LAST_START_TIME          BIGINT,
    LAST_END_TIME            BIGINT,
    LAST_EXECUTE_RESULT      VARCHAR,
    LAST_EXECUTE_RESULT_DESC VARCHAR,
    EXPECT_EXECUTE_TIME      BIGINT  not null,
    EXECUTE_NUM              INT     not null,
    EXECUTE_STATUS           VARCHAR not null,
    EXTEND_DATA              VARCHAR,
    JOB_PARAM                VARCHAR not null,
    constraint ASYNC_JOB_PK
        primary key (ID)
);

create unique index ASYNC_JOB_INDEX_1
    on ASYNC_JOB (BUSINESS_ID);

create index ASYNC_JOB_INDEX_2
    on ASYNC_JOB (EXPECT_EXECUTE_TIME, EXECUTE_STATUS);

-- ----------------------------
-- Table structure for client
-- ----------------------------
create table CLIENT
(
    ID                BIGINT  not null,
    CLIENT_ID         VARCHAR not null,
    LAST_CONNECT_TIME BIGINT  not null,
    CREATE_TIME       BIGINT  not null,
    IS_CLEAN_SESSION  VARCHAR not null,
    constraint CLIENT_PK
        primary key (ID)
);

create unique index CLIENT_INDEX_1
    on CLIENT (CLIENT_ID);

-- ----------------------------
-- Table structure for client_subscribe
-- ----------------------------
create table CLIENT_SUBSCRIBE
(
    ID             BIGINT  not null,
    CLIENT_ID      VARCHAR not null,
    TOPIC_FILTER   VARCHAR not null,
    SUBSCRIBE_TIME BIGINT  not null,
    QOS            INT     not null,
    constraint CLIENT_SUBSCRIBE_PK
        primary key (ID)
);

create index CLIENT_SUBSCRIBE_INDEX_1
    on CLIENT_SUBSCRIBE (CLIENT_ID);

create index CLIENT_SUBSCRIBE_INDEX_2
    on CLIENT_SUBSCRIBE (TOPIC_FILTER);

-- ----------------------------
-- Table structure for meta_data
-- ----------------------------
create table META_DATA
(
    KEY   VARCHAR not null,
    VALUE VARCHAR not null,
    DESC  VARCHAR not null,
    constraint META_DATA_PK
        primary key (KEY)
);

-- ----------------------------
-- Table structure for receive_qos2_message
-- ----------------------------
create table RECEIVE_QOS2_MESSAGE
(
    ID                BIGINT  not null,
    RECEIVE_QOS       INT     not null,
    TOPIC             VARCHAR not null,
    RECEIVE_PACKET_ID INT     not null,
    FROM_CLIENT_ID    VARCHAR not null,
    PAYLOAD           VARCHAR not null,
    RECEIVE_TIME      BIGINT  not null,
    IS_RETAIN         VARCHAR not null,
    constraint RECEIVE_QOS2_MESSAGE_PK
        primary key (ID)
);

create unique index RECEIVE_QOS2_MESSAGE_INDEX_1
    on RECEIVE_QOS2_MESSAGE (FROM_CLIENT_ID, RECEIVE_PACKET_ID);

-- ----------------------------
-- Table structure for retain_message
-- ----------------------------
create table RETAIN_MESSAGE
(
    ID          BIGINT  not null,
    PAYLOAD     VARCHAR not null,
    RECEIVE_QOS INT     not null,
    TOPIC       VARCHAR not null,
    constraint RETAIN_MESSAGE_PK
        primary key (ID)
);

create index RETAIN_MESSAGE_INDEX_1
    on RETAIN_MESSAGE (TOPIC);

-- ----------------------------
-- Table structure for send_message
-- ----------------------------
create table SEND_MESSAGE
(
    ID                 BIGINT  not null,
    RECEIVE_QOS        INT     not null,
    RECEIVE_PACKET_ID  INT     not null,
    FROM_CLIENT_ID     VARCHAR not null,
    SEND_QOS           INT     not null,
    TOPIC              VARCHAR not null,
    SEND_PACKET_ID     INT,
    TO_CLIENT_ID       VARCHAR not null,
    PAYLOAD            VARCHAR not null,
    IS_RECEIVE_PUB_REC VARCHAR not null,
    VALID_TIME         BIGINT  not null,
    IS_RETAIN          VARCHAR not null,
    constraint SEND_MESSAGE_PK
        primary key (ID)
);

create index SEND_MESSAGE_INDEX_1
    on SEND_MESSAGE (TO_CLIENT_ID, SEND_PACKET_ID);

create index SEND_MESSAGE_INDEX_2
    on SEND_MESSAGE (TOPIC);

create index SEND_MESSAGE_INDEX_3
    on SEND_MESSAGE (VALID_TIME);

-- ----------------------------
-- Table structure for topic_filter
-- ----------------------------
create table TOPIC_FILTER
(
    ID           BIGINT  not null,
    TOPIC_FILTER VARCHAR not null,
    constraint TOPIC_FILTER_PK
        primary key (ID)
);

create unique index TOPIC_FILTER_INDEX_1
    on TOPIC_FILTER (TOPIC_FILTER);


-- ----------------------------
-- Table structure for message_id_progress
-- ----------------------------
create table MESSAGE_ID_PROGRESS
(
    ID        BIGINT  not null,
    CLIENT_ID VARCHAR not null,
    PROGRESS  BIGINT  not null,
    constraint MESSAGE_ID_PROGRESS_PK
        primary key (ID)
);

create unique index MESSAGE_ID_PROGRESS_INDEX_1
    on MESSAGE_ID_PROGRESS (CLIENT_ID);