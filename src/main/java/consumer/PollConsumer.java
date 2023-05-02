package consumer;

import broker.Constants;
import common.*;
import models.ConsumerRecord;
import protos.Kafka;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author Alberto Delgado on 3/6/22
 * @project dsd-pub-sub
 * <p>
 * Handles the logic for polling. It creates a thread that polls continuously. So it is
 * not too heavy loaded on the broker, it has a timeout to give some time for the broker
 * to respond. If broker doesn't come back, it will request again for more records.
 * <p>
 * Upon received records, data is deserialized. To store the data, it is required for
 * the consumer to pass the storage blocking queue where it wants the records to be
 * stored.
 */
class PollConsumer<K, V> implements ClientUpdateable {
    private final BlockingQueue<ConsumerRecord<K, V>> storage; // append to the consumers storage
    private final Properties props;
    private Connection conn;
    private int offset;
    private final String topic;
    private final Serializer<K> keyDeserializer;
    private final Serializer<V> valueDeserializer;
    private Thread pollingThread;
    private ExecutorService timeout = Executors.newSingleThreadExecutor();
    Future<ConsumerRecord<K, V>> future = null;


    PollConsumer(Properties props,
                 String topic,
                 BlockingQueue<ConsumerRecord<K, V>> storage,
                 Connection conn) {
        this.props = props;
        this.topic = topic;
        this.offset = props.getConsumerOffset();
        this.storage = storage;
        this.keyDeserializer = (Serializer<K>) props.getKeyDeserializer();
        this.valueDeserializer = (Serializer<V>) props.getValueDeserializer();
        this.conn = conn;
    }

    public void setConnection(Connection conn) {
        this.conn = conn;
    }

    /**
     * Topic getter
     *
     * @return
     */
    String getTopic() {
        return topic;
    }

    /**
     * PollConsumer will keep consuming records non-stop. It has a timeout
     * so, it doesn't overwhelm too much the broker.
     */
    void poll() {
        pollingThread = new Thread(() -> {
            long delayMs = 0;
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            while (true) {
                Future<Boolean> future = scheduler.schedule(
                        () -> pollLogic(),
                        delayMs,
                        TimeUnit.MILLISECONDS
                );
                try {
                    if (future.get())
                        delayMs = 0;
                    else
                        delayMs = 1000L;
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });

        pollingThread.start();
    }

    // Handles the main polling logic
    boolean pollLogic() {
        // send request with the latest offset
        try {
            Kafka.Record proto = Kafka.Record.newBuilder()
                    .setNodeId(props.getId())
                    .setPort(props.getLocalPort())
                    .setRole(Kafka.Record.Role.CONSUMER)
                    .setType(RequestType.CONSUMER_POLL.name())
                    .setRole(Kafka.Record.Role.CONSUMER)
                    .setTopic(topic)
                    .setOffset(offset)
                    .build();

            byte[] protoBytes = proto.toByteArray();
            conn.send(protoBytes);
        } catch (IOException e) {
            conn.close();
            return false;
        }

        ConsumerRecord<K, V> record;
        while (true) {
            record = receive();
            if (record == null) return false;
            if (record.getTopic().equals(Constants.SEGMENT_HANDLER_EMPTY)) return false;
            if (record.getTopic().equals(Constants.EOT)) return true;

            System.out.println("[CONSUMER] Received offset " + offset);

            synchronized (storage) {
                int newOffset = record.getOffset(); // update offset!
                if (newOffset > offset) {
                    storage.add(record);
                    offset = newOffset;
                }
            }
        }
    }

    /**
     * Helper receive method for incoming data
     *
     * @return
     */
    private ConsumerRecord<K, V> receive() {
        ConsumerRecord<K, V> record = Receiver.receive(conn, keyDeserializer, valueDeserializer);
        return record;
    }

    /**
     * Close poll consumer
     *
     * @return
     */
    void close() {
        conn.close();
        timeout.shutdownNow();
    }
}
