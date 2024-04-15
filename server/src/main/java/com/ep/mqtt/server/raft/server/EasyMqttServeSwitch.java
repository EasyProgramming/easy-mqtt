package com.ep.mqtt.server.raft.server;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ep.mqtt.server.config.MqttClusterProperties;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * @author : zbz
 * @date : 2024/4/14
 */
@Slf4j
@Component
public class EasyMqttServeSwitch implements ApplicationRunner, DisposableBean {

    @Autowired
    private MqttClusterProperties mqttClusterProperties;

    private EasyMqttRaftServer easyMqttRaftServer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, RaftPeer> allPeerMap = getAllPeerList();

        UUID uuid = UUID.randomUUID();
        RaftGroup raftGroup = RaftGroup.valueOf(RaftGroupId.valueOf(uuid), allPeerMap.values());

        RaftPeer currentPeer = allPeerMap.get(mqttClusterProperties.getCurrentNode().getId());
        File storageDir = new File("/var/log/easy-mqtt/raft/" + currentPeer.getId());

        easyMqttRaftServer = new EasyMqttRaftServer(currentPeer, storageDir, raftGroup);
        easyMqttRaftServer.start();
        EasyMqttRaftClient.init(raftGroup);
    }

    @Override
    public void destroy() throws Exception {
        easyMqttRaftServer.close();
        EasyMqttRaftClient.close();
    }

    private Map<String, RaftPeer> getAllPeerList() {
        List<MqttClusterProperties.Node> nodeList = Lists.newArrayList();
        nodeList.add(mqttClusterProperties.getCurrentNode());
        if (!CollectionUtils.isEmpty(mqttClusterProperties.getOtherNodes())) {
            nodeList.addAll(mqttClusterProperties.getOtherNodes());
        }
        if (CollectionUtils.isEmpty(nodeList)) {
            throw new IllegalArgumentException("no cluster");
        }

        Map<String, RaftPeer> raftPeerMap = Maps.newHashMap();
        for (MqttClusterProperties.Node node : nodeList) {
            if (raftPeerMap.get(node.getId()) != null) {
                throw new IllegalArgumentException("id is repeat");
            }
            raftPeerMap.put(node.getId(),
                RaftPeer.newBuilder().setId(node.getId()).setAddress(node.getAddress()).setPriority(0).build());
        }
        return raftPeerMap;
    }

}
