package com.ep.mqtt.server.raft.server;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcConfigKeys;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.util.NetUtils;
import org.apache.ratis.util.TimeDuration;

import com.ep.mqtt.server.raft.RaftStateMachine;

/**
 * @author : zbz
 * @date : 2024/4/14
 */
public class EasyMqttRaftServer {

    private final RaftServer server;

    public EasyMqttRaftServer(RaftPeer peer, File storageDir, RaftGroup raftGroup) throws IOException {
        final RaftProperties properties = new RaftProperties();

        RaftServerConfigKeys.setStorageDir(properties, Collections.singletonList(storageDir));

        RaftServerConfigKeys.Read.setOption(properties, RaftServerConfigKeys.Read.Option.LINEARIZABLE);

        RaftServerConfigKeys.Read.setTimeout(properties, TimeDuration.ONE_MINUTE);

        final int port = NetUtils.createSocketAddr(peer.getAddress()).getPort();
        GrpcConfigKeys.Server.setPort(properties, port);

        final RaftStateMachine counterStateMachine = new RaftStateMachine(TimeDuration.ZERO);

        this.server = RaftServer.newBuilder().setGroup(raftGroup).setProperties(properties).setServerId(peer.getId())
            .setStateMachine(counterStateMachine).setOption(RaftStorage.StartupOption.RECOVER).build();
    }

    public void start() throws IOException {
        server.start();
    }

    public void close() throws IOException {
        server.close();
    }
}
