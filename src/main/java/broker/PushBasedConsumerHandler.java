package broker;

import common.Connection;
import protos.Kafka;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author Alberto Delgado on 4/4/22
 * @project dsd-pub-sub
 * <p>
 * Handles the push based subscribed consumers.
 * <p>
 * Every time a producer sends a record it is stored in the blocking queue.
 * A worker then is in charge of polling one record at a time and sending it
 * to each subscribed consumer.
 */
public class PushBasedConsumerHandler {
    private final HashMap<String, List<Connection>> consumers = new HashMap<>(); // sockets of subscribed consumers
    private final BlockingQueue<Kafka.Record> records = new LinkedBlockingDeque<>(); // list of records to be sent
    private final int POLL_TIMEOUT_MILLI = 800;
    private final Thread workerThread = new Thread(new Worker());
    private boolean isRunning = false;

    /**
     * Adds a new consumer to a topic subscription
     *
     * @param conn
     * @param topic
     */
    public synchronized void subscribe(Connection conn, String topic) {
        if (consumers.containsKey(topic)) {
            consumers.get(topic).add(conn);
        } else {
            List<Connection> newConsumers = new ArrayList<>();
            newConsumers.add(conn);
            consumers.put(topic, newConsumers);
        }

        // start worker if it hasn't started yet
        if (workerThread.getState().equals(Thread.State.NEW))
            workerThread.start();
    }

    // adds record to blocking queue
    public synchronized void add(Kafka.Record record) {
        records.add(record);
    }

    /**
     * Polls data from the blocking queue and checks the topic.
     * Then gets the list of consumers subscribed to that topic
     * and sends the data to each one of them.
     */
    private void poll() {
        try {
            Kafka.Record record = records.poll(POLL_TIMEOUT_MILLI, TimeUnit.MILLISECONDS);
            List<Connection> subscribedConsumers = consumers.get(record.getTopic());

            if (subscribedConsumers == null) return;
            for (Connection conn : subscribedConsumers) {
                byte[] data = record.toByteArray();
                try {
                    conn.send(data);
                } catch (IOException e) {
                    // perhaps connection is no longer available.
                    // Close connection.
                    subscribedConsumers.remove(conn);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the worker thread
     */
    public void close() {
        if (!isRunning) return;
        isRunning = false;
        try {
            workerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * As long as it is running it will poll one record and send it
     * to each consumer. Will continue doing this until stopped.
     */
    private class Worker implements Runnable {
        @Override
        public void run() {
            isRunning = true;
            while (isRunning) poll();
        }
    }
}
