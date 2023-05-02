package consumer;

import com.google.protobuf.InvalidProtocolBufferException;
import common.Connection;
import common.Serializer;
import models.ConsumerRecord;
import protos.Kafka;

/**
 * @author Alberto Delgado on 3/16/22
 * @project dsd-pub-sub
 * <p>
 * Helper class to receive (blocking) data from connection. It creates a ConsumerRecord
 * from the received data by deserializing it.
 */
public class Receiver {
    public static <K, V> ConsumerRecord<K, V> receive(Connection conn, Serializer keySerde, Serializer valueSerde) {
        byte[] data = conn.receive();
        if (data == null) return null;

        // Get data from broker response proto and deserialize key/value
        Kafka.Record proto;
        try {
            proto = Kafka.Record.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        }

        String topic = proto.getTopic();
        K key = (K) keySerde.deserialize(proto.getKey().toByteArray());
        V value = (V) valueSerde.deserialize(proto.getValue().toByteArray());
        Long timestamp = proto.getTimestamp();
        int offset = proto.getOffset();

        return new ConsumerRecord<>(topic, key, value, timestamp, offset);
    }
}
