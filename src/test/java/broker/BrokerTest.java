package broker;

import common.Properties;
import consumer.Consumer;
import models.ConsumerRecord;
import models.Node;
import models.ProducerRecord;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import producer.Producer;
import protos.Kafka;
import utils.KaggleParser;
import zookeeper.HeartbeatRecord;
import zookeeper.ZKNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * @author Alberto Delgado on 4/6/22
 * @project dsd-pub-sub
 */
public class BrokerTest {
    private static final int LEADER_PORT = 5000;
    private static final int LEADER_ZK_PORT = 5001;

    private static final int FOLLOWER_PORT = 6000;
    private static final int FOLLOWER_ZK_PORT = 6001;

    private static final int FOLLOWER_2_PORT = 7000;
    private static final int FOLLOWER_2_ZK_PORT = 7001;

    private static final int FOLLOWER_3_PORT = 8000;
    private static final int FOLLOWER_3_ZK_PORT = 8001;

    private static final int LEADER_ID = 1;
    private static final int FOLLOWER_ID = 2;
    private static final int FOLLOWER_2_ID = 3;
    private static final int FOLLOWER_3_ID = 4;

    private static final int PRODUCER_PORT = 9000;
    private static final int CONSUMER_PORT = 9001;
    private static final int PRODUCER_ID = 1001;
    private static final int CONSUMER_ID = 2001;
    private static final String TOPIC = "image";


    Broker createLeaderBroker() {
        Broker leader = new Broker(LEADER_ID, LEADER_PORT);
        leader.addZooKeeper(LEADER_ZK_PORT);
        return leader;
    }

    Broker createFollowerBroker() {
        Broker follower = new Broker(FOLLOWER_ID, FOLLOWER_PORT);
        follower.addZooKeeper(FOLLOWER_ZK_PORT);
        return follower;
    }

    Broker createFollower2Broker() {
        Broker follower2 = new Broker(FOLLOWER_2_ID, FOLLOWER_2_PORT);
        follower2.addZooKeeper(FOLLOWER_2_ZK_PORT);
        return follower2;
    }

    Broker createFollower3Broker() {
        Broker follower3 = new Broker(FOLLOWER_3_ID, FOLLOWER_3_PORT);
        follower3.addZooKeeper(FOLLOWER_3_ZK_PORT);
        return follower3;
    }

    Consumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put("id", String.valueOf(CONSUMER_ID));
        props.put("hostname", "localhost");
        props.put("port", String.valueOf(LEADER_PORT));
        props.put("local.port", String.valueOf(CONSUMER_PORT));
        props.put("key.deserializer", "STRING");
        props.put("value.deserializer", "STRING");
        props.put("poll.method.consumer", "poll.consumer");
        props.put("timeout.consumer", String.valueOf(50));
        Consumer<String, String> consumer = new Consumer<>(props);
        List<String> topics = new ArrayList<>();
        topics.add(TOPIC);
        consumer.subscribe(topics);
        return consumer;
    }

    @Test
    @DisplayName("should have a new member registered to broker")
    public void testZooKeeperMembership() {
        // Create follower broker + add zookeeper.
        // Testing to create follower first to demonstrate it will try to re-connect
        // as soon as leader comes online
        Broker follower = createFollowerBroker();
        Broker follower2 = createFollower2Broker();

        // add leader to brokers
        Node leaderNode = new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT);
        follower.addNode(leaderNode);
        follower2.addNode(leaderNode);

        // make sure connection from follower to leader is
        // going to fail on first attempt. Nonetheless, it will
        // retry and succeed after Thread.sleep
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }

        Broker leader = createLeaderBroker();

        Thread leaderThread = new Thread(leader);
        leaderThread.start();
        Thread followerThread = new Thread(follower);
        followerThread.start();
        Thread follower2Thread = new Thread(follower2);
        follower2Thread.start();

        try {
            Thread.sleep(3000);
        } catch (Exception ignored) {
        }

        assertMembershipTable(leader.getNodes());
        assertMembershipTable(follower.getNodes());
        assertMembershipTable(follower2.getNodes());

        Assertions.assertFalse(leader.getNodes().isEmpty());
        Assertions.assertFalse(follower.getNodes().isEmpty());
        Assertions.assertFalse(follower2.getNodes().isEmpty());
    }

    private void assertMembershipTable(List<ZKNode> nodes) {
        boolean hasLeader = false;
        boolean hasFollower = false;
        boolean hasFollower2 = false;
        for (ZKNode node : nodes) {
            if (node.ID == LEADER_ID) {
                Assertions.assertEquals(LEADER_PORT, node.BROKER_PORT);
                Assertions.assertEquals(LEADER_ZK_PORT, node.ZK_PORT);
                Assertions.assertEquals(LEADER_ID, node.ID);
                Assertions.assertTrue(node.getIsLeader());
                hasLeader = true;
            } else if (node.ID == FOLLOWER_ID) {
                Assertions.assertEquals(FOLLOWER_PORT, node.BROKER_PORT);
                Assertions.assertEquals(FOLLOWER_ZK_PORT, node.ZK_PORT);
                Assertions.assertEquals(FOLLOWER_ID, node.ID);
                Assertions.assertFalse(node.getIsLeader());
                hasFollower = true;
            } else if (node.ID == FOLLOWER_2_ID) {
                Assertions.assertEquals(FOLLOWER_2_PORT, node.BROKER_PORT);
                Assertions.assertEquals(FOLLOWER_2_ZK_PORT, node.ZK_PORT);
                Assertions.assertEquals(FOLLOWER_2_ID, node.ID);
                Assertions.assertFalse(node.getIsLeader());
                hasFollower2 = true;
            }
        }
        Assertions.assertTrue(hasLeader);
        Assertions.assertTrue(hasFollower);
        Assertions.assertTrue(hasFollower2);
    }

    @Test
    @DisplayName("should receive a heartbeat")
    public void testHeartbeat() {
        Broker leader = createLeaderBroker();
        Broker follower = createFollowerBroker();
        follower.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));
        Broker follower2 = createFollower2Broker();
        follower2.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));

        Thread leaderThread = new Thread(leader);
        leaderThread.start();
        Thread followerThread = new Thread(follower);
        followerThread.start();
        Thread follower2Thread = new Thread(follower2);
        follower2Thread.start();

        // give it some time to share hbs
        try {
            Thread.sleep(2500);
        } catch (Exception ignored) {
        }

        assertHeartbeats(leader.getHeartbeats(), LEADER_ID);
        assertHeartbeats(follower.getHeartbeats(), FOLLOWER_ID);
        assertHeartbeats(follower2.getHeartbeats(), FOLLOWER_2_ID);

        Assertions.assertFalse(leader.getHeartbeats().isEmpty());
        Assertions.assertFalse(follower.getHeartbeats().isEmpty());
        Assertions.assertFalse(follower2.getHeartbeats().isEmpty());
    }

    private void assertHeartbeats(List<HeartbeatRecord> heartbeats, int ID) {
        boolean hasLeaderHb = false;
        boolean hasFollowerHb = false;
        boolean hasFollower2Hb = false;
        for (HeartbeatRecord hb : heartbeats) {
            if (hb.id == LEADER_ID) {
                Assertions.assertEquals(LEADER_ID, hb.id);
                hasLeaderHb = true;
            } else if (hb.id == FOLLOWER_ID) {
                Assertions.assertEquals(FOLLOWER_ID, hb.id);
                hasFollowerHb = true;
            } else if (hb.id == FOLLOWER_2_ID) {
                Assertions.assertEquals(FOLLOWER_2_ID, hb.id);
                hasFollower2Hb = true;
            }
        }

        if (ID != LEADER_ID)
            Assertions.assertTrue(hasLeaderHb);
        if (ID != FOLLOWER_ID)
            Assertions.assertTrue(hasFollowerHb);
        if (ID != FOLLOWER_2_ID)
            Assertions.assertTrue(hasFollower2Hb);
    }

    @Test
    @DisplayName("should sync followers brokers on boot with leader")
    public void testSync() {
        Broker leader = createLeaderBroker();
        Broker follower = createFollowerBroker();
        follower.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));
        Broker follower2 = createFollower2Broker();
        follower2.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));

        // Start only leader thread!
        Thread leaderThread = new Thread(leader);
        leaderThread.start();

        // wait for leader to boot up
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }

        TestProducer producer = new TestProducer();
        int j = 0;
        int numberOfRecords = 300;
        System.out.println("Start publishing.");
        while (j < numberOfRecords) {
            producer.send();
            j++;
        }
        System.out.println("Done publishing");

        // Start followers thread !!
        Thread followerThread = new Thread(follower);
        followerThread.start();
        Thread follower2Thread = new Thread(follower2);
        follower2Thread.start();

        // Wait a bit for follower to sync on boot
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }

        int leaderOffset = -1;
        int followerOffset = -2;
        int follower2Offset = -3;

        leaderOffset = assertData(leader.segmentHandler.getOffsets());
        followerOffset = assertData(follower.segmentHandler.getOffsets());
        follower2Offset = assertData(follower2.segmentHandler.getOffsets());

        System.out.println(leaderOffset + " " + followerOffset + " " + follower2Offset);
        Assertions.assertEquals(leaderOffset, followerOffset);
        Assertions.assertEquals(leaderOffset, follower2Offset);
    }

    private int assertData(Set<Kafka.Record> records) {
        Assertions.assertFalse(records.isEmpty());
        for (Kafka.Record record : records) {
            return record.getOffset();
        }
        return 0;
    }

    @Test
    @DisplayName("should sync followers brokers (data store) on boot with leader while producer sending data")
    public void testReplicationConcurrently() throws InterruptedException {
        Broker leader = createLeaderBroker();
        Broker follower = createFollowerBroker();
        Broker follower2 = createFollower2Broker();
        follower.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));
        follower2.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));

        // Start only leader thread!
        Thread leaderThread = new Thread(leader);
        leaderThread.start();

        // wait for leader to boot up
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }

        // Create producer
        Properties props = new Properties();
        props.put("hostname", "localhost");
        props.put("port", String.valueOf(LEADER_PORT));
        props.put("key.serializer", "STRING");
        props.put("value.serializer", "STRING");
        Producer<String, String> producer = new Producer<>(props);

        String topic = "image";
        KaggleParser kp = KaggleParser.from("access.log", topic);

        // Start follower thread !!
        Thread followerThread = new Thread(follower);
        followerThread.start();
        Thread follower2Thread = new Thread(follower2);
        follower2Thread.start();

        System.out.println("[PRODUCER] Start publishing");
        int j = 0;
        int numberOfRecords = 400;
        while (j < numberOfRecords) {
            Thread.sleep(5);
            String val = kp.next();
            producer.publish(new ProducerRecord<>(topic, String.valueOf(j++), val));
        }
        System.out.println("Done publishing");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }

        int leaderOffset = -1;
        int followerOffset = -2;
        int follower2Offset = -3;

        leaderOffset = assertData(leader.segmentHandler.getOffsets());
        followerOffset = assertData(follower.segmentHandler.getOffsets());
        follower2Offset = assertData(follower2.segmentHandler.getOffsets());

        System.out.println(leaderOffset + " " + followerOffset + " " + follower2Offset);
        Assertions.assertEquals(leaderOffset, followerOffset);
        Assertions.assertEquals(leaderOffset, follower2Offset);
    }

    @Test
    @DisplayName("should replicate data to followers")
    public void testDataReplication() {
        Broker leader = createLeaderBroker();
        Broker follower = createFollowerBroker();
        Broker follower2 = createFollower2Broker();
        follower.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));
        follower2.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));

        Thread leaderThread = new Thread(leader);
        leaderThread.start();
        Thread followerThread = new Thread(follower);
        followerThread.start();
        Thread follower2Thread = new Thread(follower2);
        follower2Thread.start();


        // wait for leader to boot up
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {
        }

        Properties props = new Properties();
        props.put("hostname", "localhost");
        props.put("port", String.valueOf(LEADER_PORT));
        props.put("key.serializer", "STRING");
        props.put("value.serializer", "STRING");
        Producer<String, String> producer = new Producer<>(props);

        String topic = "image";
        KaggleParser kp = KaggleParser.from("access.log", topic);
        int j = 0;
        int numberOfRecords = 300;
        while (j < numberOfRecords) {
            String val = kp.next();
            producer.publish(new ProducerRecord<>(topic, String.valueOf(j++), val));
        }
        System.out.println("Done publishing");

        // Wait a bit for follower to sync on boot
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }

        int leaderOffset = -1;
        int followerOffset = -2;
        int follower2Offset = -3;

        leaderOffset = assertData(leader.segmentHandler.getOffsets());
        followerOffset = assertData(follower.segmentHandler.getOffsets());
        follower2Offset = assertData(follower2.segmentHandler.getOffsets());

        System.out.println(leaderOffset + " " + followerOffset + " " + follower2Offset);
        Assertions.assertEquals(leaderOffset, followerOffset);
        Assertions.assertEquals(leaderOffset, follower2Offset);
    }

    @Test
    @DisplayName("should elect new leader on leader crash")
    public void testElection() throws InterruptedException {
        Broker leader = createLeaderBroker();
        Broker follower = createFollowerBroker();
        Broker follower2 = createFollower2Broker();
        Broker follower3 = createFollower3Broker();
        follower.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));
        follower2.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));
        follower3.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));

        Thread leaderThread = new Thread(leader);
        Thread followerThread = new Thread(follower);
        Thread follower2Thread = new Thread(follower2);
        Thread follower3Thread = new Thread(follower3);
        leaderThread.start();
        followerThread.start();
        follower2Thread.start();
        follower3Thread.start();

        // We will let the leader and followers boot up
        // and send hb's to each other for a while
        Thread.sleep(3000);

        assertLeader(leader, LEADER_ID);
        assertLeader(follower, LEADER_ID);
        assertLeader(follower2, LEADER_ID);
        assertLeader(follower3, LEADER_ID);
        leader.close();

        Thread.sleep(6000);

        assertLeader(follower, FOLLOWER_ID);
        assertLeader(follower2, FOLLOWER_ID);
        assertLeader(follower3, FOLLOWER_ID);

        follower.close();
        Thread.sleep(6000);

        assertLeader(follower2, FOLLOWER_2_ID);
        assertLeader(follower3, FOLLOWER_2_ID);
    }

    private void assertLeader(Broker broker, int id) {
        Assertions.assertTrue(broker.getLeaderId() == id);
    }

    @Test
    @DisplayName("should keep clients (producer) always notified of leader")
    public void testProducerUpdate() throws InterruptedException {
        Broker leader = createLeaderBroker();
        Broker follower = createFollowerBroker();
        Broker follower2 = createFollower2Broker();
        follower.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));
        follower2.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));

        Thread leaderThread = new Thread(leader);
        Thread followerThread = new Thread(follower);
        Thread follower2Thread = new Thread(follower2);
        leaderThread.start();
        followerThread.start();
        follower2Thread.start();

        // We will let the leader and followers boot up
        // and send hb's to each other for a while
        Thread.sleep(3000);
        TestProducer producer = new TestProducer();

        producer.send();

        Thread.sleep(1000);

        Assertions.assertEquals(1,
                leader.dataStore.topics.get(TOPIC).size());
        Assertions.assertEquals(1,
                follower.dataStore.topics.get(TOPIC).size());
        Assertions.assertEquals(1,
                follower2.dataStore.topics.get(TOPIC).size());

        leader.close();
        Thread.sleep(6000);

        assertLeader(follower, FOLLOWER_ID);
        assertLeader(follower2, FOLLOWER_ID);

        producer.send();

        Thread.sleep(1000);
        Assertions.assertEquals(2,
                follower.dataStore.topics.get(TOPIC).size());
        Assertions.assertEquals(2,
                follower2.dataStore.topics.get(TOPIC).size());

        follower.close();
        Thread.sleep(6000);
        producer.send();
        Thread.sleep(1000);

        Assertions.assertEquals(3,
                follower2.dataStore.topics.get(TOPIC).size());
    }

    @Test
    @DisplayName("should keep clients (consumer) always notified of leader")
    public void testConsumer() throws InterruptedException {
        Broker leader = createLeaderBroker();
        Broker follower = createFollowerBroker();
        Broker follower2 = createFollower2Broker();
        follower.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));
        follower2.addNode(new Node(LEADER_ID, "localhost", LEADER_ZK_PORT, LEADER_PORT));

        Thread leaderThread = new Thread(leader);
        Thread followerThread = new Thread(follower);
        Thread follower2Thread = new Thread(follower2);
        leaderThread.start();
        followerThread.start();
        follower2Thread.start();

        // We will let the leader and followers boot up
        // and send hb's to each other for a while
        Thread.sleep(3000);
        TestProducer producer = new TestProducer();
        Thread.sleep(500);
        Consumer consumer = createConsumer();
        new Thread(consumer).start();

        for (int i = 0; i < 100; i++) {
            producer.send();
            // give time for broker to persist
            Thread.sleep(5);
        }

        Thread.sleep(1000);

        Assertions.assertEquals(0,
                leader.dataStore.topics.get(TOPIC).size());
        Assertions.assertEquals(0,
                follower.dataStore.topics.get(TOPIC).size());
        Assertions.assertEquals(0,
                follower2.dataStore.topics.get(TOPIC).size());

        List<ConsumerRecord<String, String>> records = consumer.poll(Duration.ofMillis(500));
        Assertions.assertEquals(100, records.size());

        leader.close();
        Thread.sleep(6000);

        assertLeader(follower, FOLLOWER_ID);
        assertLeader(follower2, FOLLOWER_ID);

        for (int i = 0; i < 100; i++) {
            producer.send();
            // give time for broker to persist
            Thread.sleep(5);
        }

        Thread.sleep(1000);

        Assertions.assertEquals(0,
                follower.dataStore.topics.get(TOPIC).size());
        Assertions.assertEquals(0,
                follower2.dataStore.topics.get(TOPIC).size());

        records = consumer.poll(Duration.ofMillis(500));
        Assertions.assertEquals(100, records.size());

        follower.close();
        Thread.sleep(6000);

        for (int i = 0; i < 100; i++) {
            producer.send();
            // give time for broker to persist
            Thread.sleep(5);
        }

        Thread.sleep(1000);
        records = consumer.poll(Duration.ofMillis(500));
        Assertions.assertEquals(100, records.size());
    }

    private static class TestProducer {
        Producer<String, String> producer;
        KaggleParser kp;
        int counter = 0;
        String topic;

        TestProducer() {
            Properties props = new Properties();
            props.put("id", String.valueOf(PRODUCER_ID));
            props.put("hostname", "localhost");
            props.put("port", String.valueOf(LEADER_PORT));
            props.put("local.port", String.valueOf(PRODUCER_PORT));
            props.put("key.serializer", "STRING");
            props.put("value.serializer", "STRING");
            producer = new Producer<>(props);
            topic = TOPIC;
            kp = KaggleParser.from("access.log", topic);
            Executors.newSingleThreadExecutor().submit(producer);
        }

        void send() {
            String val = kp.next();
            producer.publish(new ProducerRecord<>(topic, String.valueOf(counter++), val));
        }
    }
}