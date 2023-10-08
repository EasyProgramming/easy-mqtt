package com.ep.mqtt.server.listener.msg;

import java.util.List;

import com.ep.mqtt.server.vo.TopicVo;

import lombok.Data;

/**
 * @author : zbz
 * @date : 2023/8/21
 */
@Data
public class ManageTopicFilterMsg {

    /**
     * @see ManageType
     */
    private String manageType;

    private String clientId;

    private List<TopicVo> topicVoList;

    public enum ManageType {

        /**
         * 订阅
         */
        SUBSCRIBE("subscribe", "订阅"),

        /**
         * 取消订阅
         */
        UN_SUBSCRIBE("un_subscribe", "取消订阅"),;

        private final String key;

        private final String desc;

        ManageType(String key, String desc) {
            this.key = key;
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }

        public String getKey() {
            return key;
        }

    }

}
