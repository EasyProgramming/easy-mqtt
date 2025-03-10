package com.ep.mqtt.server.raft.client;

import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroup;

import java.io.IOException;

/**
 * @author : zbz
 * @date : 2024/4/15
 */
public class EasyMqttRaftClient {

    private static RaftClient client = null;

    public static void init(RaftGroup raftGroup) {
        client = RaftClient.newBuilder().setProperties(new RaftProperties()).setRaftGroup(raftGroup).build();
    }

    public static void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    public static void syncSend(String data) {
        try {
            client.io().send(Message.valueOf(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
