package producer;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import common.Client;
import common.Properties;
import common.RequestType;
import common.Serializer;
import models.ProducerRecord;
import protos.Kafka;
import utils.Demo;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.*;

/**
 * @author Alberto Delgado on 2/28/22
 * @project dsd-pub-sub
 * <p>
 * Producer: sends data to a broker by specifying the topic and connection details.
 * Works synchronously, after each package sent it expects an ACK
 */
public class Producer<K, V> extends Client {
    final Serializer<K> keySerializer;
    final Serializer<V> valueSerializer;

    /**
     * Creates connection upon object creation
     *
     * @param props
     */
    public Producer(Properties props) {
        super(props);
        keySerializer = (Serializer<K>) props.getKeySerializer();
        valueSerializer = (Serializer<V>) props.getValueSerializer();

        System.out.println("[PRODUCER] Running on port " + props.getLocalPort());
        System.out.println("[PRODUCER] Connecting to " + props.getHostname() + ":" + props.getPort());
    }

    // Sends data and receives ACK
    public synchronized boolean publish(ProducerRecord<K, V> data) {
        boolean sent = send(data);
        if (!sent)
            return false;

        byte[] ackBytes = receive();
        if (ackBytes == null) return publish(data);

        Kafka.Record ack;
        try {
            ack = Kafka.Record.parseFrom(ackBytes);
        } catch (InvalidProtocolBufferException e) {
            return false;
        }
        return Arrays.equals(keySerializer.serialize(data.getKey()), ack.getKey().toByteArray());
    }

    // Receives data
    private byte[] receive() {
        ExecutorService timeout = Executors.newSingleThreadExecutor();
        Future<byte[]> future = timeout.submit(conn::receive);
        long timeoutMs = 400L;

        while (true) {
            try {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                continue;
            } catch (InterruptedException e) {
                continue;
            } catch (TimeoutException e) {
                // re-attempt
                continue;
            }
        }
    }

    /**
     * Sends data
     *
     * @param data
     * @return
     */
    private boolean send(ProducerRecord<K, V> data) {
        if (!isConnected()) {
            Demo.printAndDelay("[PRODUCER] Connection with Broker not established. Solve conflict.");
            conn.reconnect();
            return send(data);
        }

        Kafka.Record proto = Kafka.Record.newBuilder()
                .setNodeId(ID)
                .setPort(PORT)
                .setRole(Kafka.Record.Role.PRODUCER)
                .setType(RequestType.PRODUCER_PUBLISH.name())
                .setTopic(data.getTopic())
                .setKey(ByteString.copyFrom(keySerializer.serialize(data.getKey())))
                .setValue(ByteString.copyFrom(valueSerializer.serialize(data.getValue())))
                .setTimestamp(data.getTimestamp())
                .build();

        byte[] protoBytes = proto.toByteArray();

        try {
            conn.send(protoBytes);
            return true;
        } catch (IOException e) {
            Demo.printAndDelay("Could not send data. Re-trying");
            return send(data);
        }
    }

    /**
     * Helper method to check connection status
     *
     * @return
     */
    private boolean isConnected() {
        if (conn == null) return false;
        if (conn.isClosed()) return false;
        if (!conn.hasConnected) return false;
        return true;
    }
}
