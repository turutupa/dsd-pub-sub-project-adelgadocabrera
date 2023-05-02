package models;

import java.util.Objects;

/**
 * @author Alberto Delgado on 2/28/22
 * @project dsd-pub-sub
 * <p>
 * Records created by the producer.
 */
public class ProducerRecord<K, V> extends Record<K, V> {
    public ProducerRecord(String topic, K key, V value) {
        this(topic, key, value, null);
    }

    /**
     * If timestamp is not specified by producer then it is
     * generated automatically
     *
     * @param topic
     * @param key
     * @param value
     * @param timestamp
     */
    public ProducerRecord(String topic, K key, V value, Long timestamp) {
        super(topic, key, value);
        this.topic = topic;
        this.key = key;
        this.value = value;
        this.timestamp = Objects.requireNonNullElseGet(timestamp, System::currentTimeMillis);
    }
}
