package com.ep.mqtt.server.raft.client;

import com.ep.mqtt.server.metadata.Constant;
import com.ep.mqtt.server.raft.server.EasyMqttRaftServeSwitch;
import com.ep.mqtt.server.util.ReadWriteLockUtil;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftPeer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author : zbz
 * @date : 2024/4/15
 */
@Slf4j
public class EasyMqttRaftClient {

    private final static ReadWriteLockUtil REFRESH_PEER_LOCK = new ReadWriteLockUtil();

    private static RaftClient client = null;

    private static List<RaftPeer> raftPeerList = Lists.newArrayList();

    /**
     * 定时刷新节点信息的线程池
     */
    private static final ScheduledThreadPoolExecutor REFRESH_PEER_THREAD_POOL =
            new ScheduledThreadPoolExecutor(Constant.PROCESSOR_NUM, new ThreadFactoryBuilder().setNameFormat("refresh-peer-%s").build());

    public static void init(RaftGroup raftGroup) {
        client = RaftClient.newBuilder().setProperties(new RaftProperties()).setRaftGroup(raftGroup).build();

        REFRESH_PEER_THREAD_POOL.scheduleWithFixedDelay(()->{
            REFRESH_PEER_LOCK.writeLock(()->{
                try {
                    Collection<RaftPeer> peers = client.getGroupManagementApi(client.getLeaderId()).info(EasyMqttRaftServeSwitch.RAFT_GROUP_ID).getGroup().getPeers();
                    raftPeerList = Lists.newArrayList(peers);
                }
                catch (Throwable e){
                    log.error("刷新raft节点出错", e);
                }

                log.info("raft节点信息:[{}]", raftPeerList);
            });
        }, 60, 100, TimeUnit.SECONDS);
    }

    public static void close() throws IOException {
        if (client != null) {
            client.close();
        }

        REFRESH_PEER_THREAD_POOL.shutdown();
    }

    public static void syncSend(String data) {
        try {
            client.io().send(Message.valueOf(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void broadcast(String data) {
        REFRESH_PEER_LOCK.readLock(()->{
            for (RaftPeer raftPeer : raftPeerList){
                client.async().sendReadOnlyUnordered(Message.valueOf(data), raftPeer.getId());
            }

            return null;
        });
    }

}
