package com.ep.mqtt.server.vo;

import lombok.Data;

/**
 * @author : zbz
 * @date : 2023/7/22
 */
@Data
public class TopicVo {

    private Integer qos;

    private String topicFilter;

}
