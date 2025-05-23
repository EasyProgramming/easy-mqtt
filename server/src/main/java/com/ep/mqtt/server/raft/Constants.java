package com.ep.mqtt.server.raft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.util.JavaUtils;
import org.apache.ratis.util.TimeDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : zbz
 * @date : 2024/4/14
 */
public class Constants {
    private static final Logger LOG = LoggerFactory.getLogger(Constants.class);

    private static final String CONF_FILE_NAME = "conf.properties";
    private static final List<String> CONF_FILE_DEFAULTS =
        Collections.unmodifiableList(Arrays.asList("examples/conf/" + CONF_FILE_NAME, // for release tarball layout
            "ratis-examples/src/main/resources/" + CONF_FILE_NAME)); // for source tree layout
    private static final String CONF_FILE_ENV_VAR_NAME = "RATIS_EXAMPLE_CONF";

    static Path getConfPath() {
        final String env = JavaUtils.getEnv(CONF_FILE_ENV_VAR_NAME);
        final Stream<String> s = Stream.concat(Optional.ofNullable(env).map(Stream::of).orElseGet(Stream::empty),
            CONF_FILE_DEFAULTS.stream());
        for (final Iterator<String> i = s.iterator(); i.hasNext();) {
            final Path p = Paths.get(i.next());
            if (Files.exists(p)) {
                LOG.info("Using conf file {}", p);
                return p;
            }
        }
        throw new IllegalArgumentException(
            "Conf file not found: please set environment variable \"" + CONF_FILE_ENV_VAR_NAME + "\"");
    }

    public static final List<RaftPeer> PEERS;
    public static final String PATH;
    public static final List<TimeDuration> SIMULATED_SLOWNESS;

    static {
        final Properties properties = new Properties();
        final Path conf = getConfPath();
        try (BufferedReader in =
            new BufferedReader(new InputStreamReader(Files.newInputStream(conf), StandardCharsets.UTF_8))) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + conf, e);
        }
        Function<String, String[]> parseConfList =
            confKey -> Optional.ofNullable(properties.getProperty(confKey)).map(s -> s.split(",")).orElse(null);
        final String key = "raft.server.address.list";
        final String[] addresses = parseConfList.apply(key);
        if (addresses == null || addresses.length == 0) {
            throw new IllegalArgumentException("Failed to get " + key + " from " + conf);
        }

        final String priorityKey = "raft.server.priority.list";
        final String[] priorities = parseConfList.apply(priorityKey);
        if (priorities != null && priorities.length != addresses.length) {
            throw new IllegalArgumentException("priority should be assigned to each server in " + conf);
        }

        final String slownessKey = "raft.server.simulated-slowness.list";
        final String[] slowness = parseConfList.apply(slownessKey);
        if (slowness != null && slowness.length != addresses.length) {
            throw new IllegalArgumentException("simulated-slowness should be assigned to each server in" + conf);
        }
        SIMULATED_SLOWNESS = slowness == null ? null
            : Arrays.stream(slowness).map(s -> TimeDuration.valueOf(s, TimeUnit.SECONDS)).collect(Collectors.toList());

        final String key1 = "raft.server.root.storage.path";
        final String path = properties.getProperty(key1);
        PATH = path == null ? "./ratis-examples/target" : path;
        final List<RaftPeer> peers = new ArrayList<>(addresses.length);
        for (int i = 0; i < addresses.length; i++) {
            final int priority = priorities == null ? 0 : Integer.parseInt(priorities[i]);
            peers.add(RaftPeer.newBuilder().setId("n" + i).setAddress(addresses[i]).setPriority(priority).build());
        }
        PEERS = Collections.unmodifiableList(peers);
    }

    private static final UUID GROUP_ID = UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1");

    public static final RaftGroup RAFT_GROUP = RaftGroup.valueOf(RaftGroupId.valueOf(Constants.GROUP_ID), PEERS);

    private Constants() {}
}
