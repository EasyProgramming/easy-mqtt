package com.ep.mqtt.server.raft.server;

import com.ep.mqtt.server.config.MqttServerProperties;
import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.raft.client.EasyMqttRaftClient;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author : zbz
 * @date : 2024/4/14
 */
@Slf4j
@Component
public class EasyMqttRaftServeSwitch {

    public static RaftGroupId RAFT_GROUP_ID = RaftGroupId.valueOf(ByteString.copyFrom("easy-mqtt-000000", StandardCharsets.UTF_8));

    private EasyMqttRaftServer easyMqttRaftServer;

    @Resource
    private MqttServerProperties mqttServerProperties;

    public void start() throws Exception {
        long start = System.currentTimeMillis();
        log.info("start raft server");

        String[] nodeIps = StringUtils.split(mqttServerProperties.getNodeIp(), Constant.ENGLISH_COMMA);

        Map<String, RaftPeer> allPeerMap = getAllPeerList(nodeIps, mqttServerProperties.getRaftPort());

        RaftGroup raftGroup =
            RaftGroup.valueOf(RAFT_GROUP_ID, allPeerMap.values());

        RaftPeer currentPeer = allPeerMap.get(nodeIps[0]);

        File storageDir = new File(Constant.PROJECT_BASE_DIR + "/raft/" + currentPeer.getId());
        storageDir.mkdirs();

        easyMqttRaftServer = new EasyMqttRaftServer(currentPeer, storageDir, raftGroup);
        easyMqttRaftServer.start();
        EasyMqttRaftClient.init(raftGroup);

        log.info("complete start raft server, cost [{}ms]", System.currentTimeMillis() - start);
    }

    public void stop() throws Exception {
        long start = System.currentTimeMillis();
        log.info("stop raft server");

        easyMqttRaftServer.close();
        EasyMqttRaftClient.close();

        log.info("complete stop raft server, cost [{}ms]", System.currentTimeMillis() - start);
    }

    private Map<String, RaftPeer> getAllPeerList(String[] nodeIps, Integer raftPort) {
        Map<String, RaftPeer> raftPeerMap = Maps.newHashMap();
        for (String nodeIp : nodeIps) {
            if (raftPeerMap.get(nodeIp) != null) {
                throw new IllegalArgumentException("id is repeat");
            }

            raftPeerMap.put(nodeIp, RaftPeer.newBuilder().setId(nodeIp).setAddress(nodeIp + ":" + raftPort).build());
        }
        return raftPeerMap;
    }

}
