package com.ep.mqtt.server.raft.server;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * @author : zbz
 * @date : 2024/4/14
 */
@Slf4j
@Component
public class EasyMqttServeSwitch implements ApplicationRunner, DisposableBean {

    private EasyMqttRaftServer easyMqttRaftServer;

    @Resource
    private MqttServerProperties mqttServerProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String[] nodeAddresses = StringUtils.split(mqttServerProperties.getNodeAddress(), Constant.ENGLISH_COMMA);

        Map<String, RaftPeer> allPeerMap = getAllPeerList(nodeAddresses);

        RaftGroup raftGroup =
            RaftGroup.valueOf(RaftGroupId.valueOf(ByteString.copyFrom("easy-mqtt".getBytes(StandardCharsets.UTF_8))), allPeerMap.values());

        RaftPeer currentPeer = allPeerMap.get(nodeAddresses[0]);

        File storageDir = new File(Constant.PROJECT_BASE_DIR + "/raft/" + currentPeer.getId());

        easyMqttRaftServer = new EasyMqttRaftServer(currentPeer, storageDir, raftGroup);
        easyMqttRaftServer.start();
        EasyMqttRaftClient.init(raftGroup);
    }

    @Override
    public void destroy() throws Exception {
        easyMqttRaftServer.close();
        EasyMqttRaftClient.close();
    }

    private Map<String, RaftPeer> getAllPeerList(String[] nodeAddresses) {
        Map<String, RaftPeer> raftPeerMap = Maps.newHashMap();
        for (String nodeAddress : nodeAddresses) {
            if (raftPeerMap.get(nodeAddress) != null) {
                throw new IllegalArgumentException("id is repeat");
            }

            raftPeerMap.put(nodeAddress, RaftPeer.newBuilder().setId(nodeAddress).setAddress(nodeAddress).build());
        }
        return raftPeerMap;
    }

}
