package com.ep.mqtt.server.listener.msg;

import com.ep.mqtt.server.vo.MessageVo;

import lombok.Data;

/**
 * @author : zbz
 * @date : 2023/8/21
 */
@Data
public class ManageRetainMessageMsg {

    /**
     * @see ManageType
     */
    private String manageType;

    private MessageVo messageVo;

    public enum ManageType {

        /**
         * 新增/更新
         */
        ADD("add", "新增/更新"),

        /**
         * 删除
         */
        REMOVE("remove", "删除"),;

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
