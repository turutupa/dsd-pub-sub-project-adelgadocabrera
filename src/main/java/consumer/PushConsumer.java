package consumer;

import common.Connection;
import common.Properties;
import common.Serializer;
import models.ConsumerRecord;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Alberto Delgado on 3/6/22
 * @project dsd-pub-sub
 * <p>
 * Push Consumer logic. It listens for any new records broker may have, and stores
 * them into the Consumer storage. Push Consumer will deserialize the records upon
 * receiving them.
 */
class PushConsumer<K, V> {
    private boolean running = true;
    private final Connection conn;
    private final Serializer keyDeserializer;
    private final Serializer valueDeserializer;
    private final BlockingQueue<ConsumerRecord<K, V>> storage;
    private final ExecutorService receivingThread = Executors.newSingleThreadExecutor();

    PushConsumer(
            Connection conn,
            Properties props,
            BlockingQueue<ConsumerRecord<K, V>> storage) {
        this.storage = storage;
        this.keyDeserializer = props.getKeyDeserializer();
        this.valueDeserializer = props.getValueDeserializer();
        this.conn = conn;

        // Start listening for records.
        start();
    }

    /**
     * Starts listening for new records.
     */
    private void start() {
        receivingThread.execute(() -> {
            while (running) {
                ConsumerRecord<K, V> record = Receiver.receive(conn, keyDeserializer, valueDeserializer);
                if (record != null) storage.add(record);
            }
        });
    }

    /**
     * Closes the Push Consumer.
     *
     * @return
     */
    void close() {
        running = false;
        conn.close();
        receivingThread.shutdownNow();
    }
}
