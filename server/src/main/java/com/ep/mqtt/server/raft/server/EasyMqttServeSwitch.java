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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
public class EasyMqttServeSwitch implements ApplicationRunner, DisposableBean {

    private EasyMqttRaftServer easyMqttRaftServer;

    @Resource
    private MqttServerProperties mqttServerProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String[] nodeAddresses = StringUtils.split(mqttServerProperties.getNodeAddress(), Constant.ENGLISH_COMMA);

        Map<String, RaftPeer> allPeerMap = getAllPeerList(nodeAddresses);

        RaftGroup raftGroup =
            RaftGroup.valueOf(RaftGroupId.valueOf(ByteString.copyFrom("easy-mqtt-000000", StandardCharsets.UTF_8)), allPeerMap.values());

        RaftPeer currentPeer = allPeerMap.get(getId(nodeAddresses[0]));

        File storageDir = new File(Constant.PROJECT_BASE_DIR + "/raft/" + currentPeer.getId());
        storageDir.mkdirs();

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
            String id = getId(nodeAddress);

            if (raftPeerMap.get(id) != null) {
                throw new IllegalArgumentException("id is repeat");
            }

            raftPeerMap.put(id, RaftPeer.newBuilder().setId(id).setAddress(nodeAddress).build());
        }
        return raftPeerMap;
    }

    private String getId(String nodeAddress){
        return StringUtils.replace(nodeAddress, ":", "-");
    }
}
