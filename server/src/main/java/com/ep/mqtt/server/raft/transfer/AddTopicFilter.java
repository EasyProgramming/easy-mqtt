package com.ep.mqtt.server.raft.transfer;

import lombok.Data;

import java.util.Set;

/**
 * @author zbz
 * @date 2025/2/21 14:20
 */
@Data
public class AddTopicFilter {

    private Set<String> topicFilterSet;

}
