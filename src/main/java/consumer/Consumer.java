package consumer;

import common.Client;
import common.Connection;
import common.Properties;
import common.RequestType;
import models.ConsumerRecord;
import protos.Kafka;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Alberto Delgado on 2/28/22
 * @project dsd-pub-sub
 * <p>
 * Consumer. There are two types:
 * - Poll consumer: polls data from the broker with a specified timeout
 * - Push consumer: subscribes to a specified topic
 * <p>
 * Data is stored in a blocking queue and a poll method is used to extract data
 * from the blocking queue.
 * <p>
 * It is advised to close the consumer if not required at a certain point to
 * prevent memory leaks.
 */
public class Consumer<K, V> extends Client {
    // This is where the records are stored
    private final BlockingQueue<ConsumerRecord<K, V>> recordsQueue = new LinkedBlockingDeque<>();
    private final Properties props;
    private final Set<String> topics = new HashSet<>();
    // Each topic is being polled by a PollingConsumer. We store these not only
    // to be able to close them when done, but to keep track of the polled topics.
    // Same strategy is used for push consumers.
    private final Set<PollConsumer<K, V>> pollingConsumers = new HashSet<>();
    private final Set<PushConsumer<K, V>> pushConsumers = new HashSet<>();

    public Consumer(Properties props) {
        super(props);
        this.props = props;

        System.out.println("[CONSUMER] Running on port " + props.getLocalPort());
        System.out.println("[CONSUMER] Connecting to " + props.getHostname() + ":" + props.getPort());
    }

    /**
     * Subscribes to:
     * - In case of poll consumer: creates PollConsumer
     * - In case of push consumer: creates PushConsumer
     *
     * @param subscriptions
     * @return
     */
    public boolean subscribe(Collection<String> subscriptions) {
        if (props.getConsumerMethod().equals(Properties.POLL_CONSUMER)) {
            pollSubscription(subscriptions);
        } else if (props.getConsumerMethod().equals(Properties.PUSH_CONSUMER)) {
            pushSubscription(props.getHostname(), props.getPort(), subscriptions);
        } else {
            return false;
        }

        return true;
    }

    /**
     * Poll consumer type:
     * Adds topics to the list of topics to be polled.
     * Allows new subscriptions to happen even after polls
     * have already started for other topics
     *
     * @param subscriptions topics to subscribe to
     * @return boolean
     */
    public boolean pollSubscription(Collection<String> subscriptions) {
        topics.addAll(subscriptions);
        pollTopics(); // make sure every topic is being polled

        return true;
    }

    /**
     * Create a thread for each topic being polled. If a topic
     * is already in the pollingTopics set means it is already
     * being fetched, and we avoid creating a duplicate thread
     * for that topic.
     */
    private void pollTopics() {
        // do not poll if consumer type is push
        if (props.getConsumerMethod().equals(Properties.PUSH_CONSUMER)) return;

        for (String topic : topics) {
            // First avoid having duplicate poll consumers for the
            // same topic.
            boolean isBeingPolled = false;
            for (PollConsumer<K, V> consumer : pollingConsumers) {
                if (consumer.getTopic().equals(topic)) {
                    isBeingPolled = true;
                    break;
                }
            }
            if (isBeingPolled) continue;

            PollConsumer<K, V> pollConsumer = new PollConsumer<>(props, topic, recordsQueue, conn);
            pollingConsumers.add(pollConsumer);
            pollConsumer.poll();
        }
    }

    /**
     * Push consumer type:
     * It subscribes to a broker so broker will send data to
     * this consumer as soon as the broker gets new records.
     *
     * @param conn connection to broker
     * @return boolean
     */
    private boolean sendSubscriptionRequest(Connection conn, String topic) {
        try {
            Kafka.Record proto = Kafka.Record.newBuilder()
                    .setNodeId(ID)
                    .setPort(props.getLocalPort())
                    .setRole(Kafka.Record.Role.CONSUMER)
                    .setType(RequestType.CONSUMER_SUBSCRIBE.name())
                    .setTopic(topic)
                    .build();

            byte[] protoBytes = proto.toByteArray();
            conn.send(protoBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Subscription method to a topic. Requires broker information to connect to.
     *
     * @param hostname
     * @param port
     * @param subscriptions
     * @return
     */
    public boolean pushSubscription(String hostname, int port, Collection<String> subscriptions) {
        boolean subscribedToAll = true;
        for (String topic : subscriptions) {
            Connection conn = new Connection(hostname, port);
            boolean subscribed = sendSubscriptionRequest(conn, topic);
            if (!subscribed) {
                subscribedToAll = false;
                continue;
            }
            PushConsumer<K, V> pushConsumer = new PushConsumer(conn, props, recordsQueue);
            pushConsumers.add(pushConsumer);
        }

        return subscribedToAll;
    }

    /**
     * Allows client to poll from already stored messages
     */
    public ArrayList<ConsumerRecord<K, V>> poll(Duration duration) {
        ArrayList<ConsumerRecord<K, V>> records = new ArrayList<>();
        synchronized (recordsQueue) {
            while (!recordsQueue.isEmpty()) {
                ConsumerRecord<K, V> record = recordsQueue.poll();
                if (record != null) records.add(record);
            }
        }

        return records;
    }

    /**
     * Closes all current polls
     */
    public void close() {
        for (PollConsumer<K, V> pollConsumer : pollingConsumers) {
            pollConsumer.close();
        }

        for (PushConsumer<K, V> pushConsumer : pushConsumers) {
            pushConsumer.close();
        }
    }

    @Override
    public void setConnection(Connection conn) {
        this.conn = conn;
        pollingConsumers.forEach(c -> c.setConnection(conn));
    }
}

