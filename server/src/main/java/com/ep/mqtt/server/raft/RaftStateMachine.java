package com.ep.mqtt.server.raft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.ratis.io.MD5Hash;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientRequest;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.protocol.TermIndex;
import org.apache.ratis.server.storage.FileInfo;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.apache.ratis.statemachine.impl.SimpleStateMachineStorage;
import org.apache.ratis.statemachine.impl.SingleFileSnapshotInfo;
import org.apache.ratis.util.MD5FileUtil;

import com.ep.mqtt.server.metadata.RaftCommand;
import com.ep.mqtt.server.raft.transfer.TransferData;
import com.ep.mqtt.server.store.TopicFilterStore;
import com.ep.mqtt.server.store.TopicStore;
import com.google.common.collect.Sets;

import io.vertx.core.impl.ConcurrentHashSet;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zbz
 * @date 2024/4/8 18:25
 */
@Slf4j
public class RaftStateMachine extends BaseStateMachine {

    static class State {

        private final TermIndex applied;

        private final ConcurrentHashSet<String> topicFilterSet = new ConcurrentHashSet<>();

        private final ConcurrentHashSet<String> topicSet = new ConcurrentHashSet<>();

        State(TermIndex applied, ConcurrentHashSet<String> topicFilterSet, ConcurrentHashSet<String> topicSet) {
            this.applied = applied;
            this.topicFilterSet.addAll(topicFilterSet);
            this.topicSet.addAll(topicSet);
        }

        TermIndex getApplied() {
            return applied;
        }

        public ConcurrentHashSet<String> getTopicFilterSet() {
            return topicFilterSet;
        }

        public ConcurrentHashSet<String> getTopicSet() {
            return topicSet;
        }
    }

    private final static String SPLIT_FLAG = "--split--";

    private final SimpleStateMachineStorage storage = new SimpleStateMachineStorage();

    /**
     * 执行状态机前的方法，用于处理一些参数校验
     */
    @Override
    public TransactionContext startTransaction(RaftClientRequest request) throws IOException {
        final TransactionContext transaction = super.startTransaction(request);
        String content = request.getMessage().getContent().toString(StandardCharsets.UTF_8);
        TransferData transferData = TransferData.convert(content);

        if (transferData == null) {
            transaction.setException(new IllegalArgumentException("invalid data format: " + content));
            return transaction;
        }

        if (RaftCommand.get(transferData.getCommand()) == null) {
            transaction.setException(new IllegalArgumentException("invalid command: " + content));
        }
        return transaction;
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        TransferData transferData =
            TransferData.convert(trx.getClientRequest().getMessage().getContent().toString(StandardCharsets.UTF_8));
        RaftCommand command = RaftCommand.get(transferData.getCommand());
        final TermIndex termIndex = TermIndex.valueOf(trx.getLogEntry());

        synchronized (this) {
            // noinspection ConstantConditions
            switch (command) {
                case ADD_TOPIC_FILTER:
                    TopicFilterStore.add(transferData.getData());
                    break;
                case REMOVE_TOPIC_FILTER:
                    TopicFilterStore.remove(transferData.getData());
                    break;
                case ADD_TOPIC:
                    TopicStore.add(transferData.getData());
                    break;
                case REMOVE_TOPIC:
                    TopicStore.remove(transferData.getData());
                    break;
                default:
            }

            updateLastAppliedTermIndex(termIndex);
        }
        return CompletableFuture.completedFuture(Message.EMPTY);
    }

    @Override
    public long takeSnapshot() {
        // get the current state
        final State state = getState();
        final long index = state.getApplied().getIndex();

        // create a file with a proper name to store the snapshot
        final File snapshotFile = storage.getSnapshotFile(state.getApplied().getTerm(), index);

        // write the counter value into the snapshot file
        try (BufferedWriter out = Files.newBufferedWriter(snapshotFile.toPath())) {
            for (String topicFilter : state.getTopicFilterSet()) {
                out.write(topicFilter);
                out.newLine();
            }
            // 写入分割行
            out.write(SPLIT_FLAG);
            out.newLine();

            for (String topic : state.getTopicSet()) {
                out.write(topic);
                out.newLine();
            }
        } catch (IOException ioe) {
            log.warn(
                "Failed to write snapshot file \"" + snapshotFile + "\", last applied index=" + state.getApplied());
        }

        // update storage
        final MD5Hash md5 = MD5FileUtil.computeAndSaveMd5ForFile(snapshotFile);
        final FileInfo info = new FileInfo(snapshotFile.toPath(), md5);
        storage.updateLatestSnapshot(new SingleFileSnapshotInfo(info, state.getApplied()));

        // return the index of the stored snapshot (which is the last applied one)
        return index;
    }

    private void load(SingleFileSnapshotInfo snapshot) throws IOException {
        // check null
        if (snapshot == null) {
            return;
        }
        // check if the snapshot file exists.
        final Path snapshotPath = snapshot.getFile().getPath();
        if (!Files.exists(snapshotPath)) {
            log.warn("The snapshot file {} does not exist for snapshot {}", snapshotPath, snapshot);
            return;
        }

        // verify md5
        final MD5Hash md5 = snapshot.getFile().getFileDigest();
        if (md5 != null) {
            MD5FileUtil.verifySavedMD5(snapshotPath.toFile(), md5);
        }

        // read the TermIndex from the snapshot file name
        final TermIndex last = SimpleStateMachineStorage.getTermIndexFromSnapshotFile(snapshotPath.toFile());

        // read the counter value from the snapshot file
        final Set<String> topicFilterSet = Sets.newHashSet();
        final Set<String> topicSet = Sets.newHashSet();
        boolean isTopic = false;
        try (BufferedReader in = Files.newBufferedReader(snapshotPath)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals(SPLIT_FLAG)) {
                    isTopic = true;
                    continue;
                }

                if (isTopic) {
                    topicSet.add(line);
                } else {
                    topicFilterSet.add(line);
                }
            }
        }

        // update state
        updateState(last, topicFilterSet, topicSet);
    }

    @Override
    public void initialize(RaftServer server, RaftGroupId groupId, RaftStorage raftStorage) throws IOException {
        super.initialize(server, groupId, raftStorage);
        storage.init(raftStorage);
        reinitialize();
    }

    @Override
    public void reinitialize() throws IOException {
        load(storage.getLatestSnapshot());
    }

    private synchronized State getState() {
        return new State(getLastAppliedTermIndex(), TopicFilterStore.getTopicFilterSet(), TopicStore.getTopicSet());
    }

    private synchronized void updateState(TermIndex applied, Set<String> topicFilterSet, Set<String> topicSet) {
        updateLastAppliedTermIndex(applied);

        // 初始化数据
        TopicStore.getTopicSet().addAll(topicSet);
        TopicFilterStore.getTopicFilterSet().addAll(topicFilterSet);
    }
}
