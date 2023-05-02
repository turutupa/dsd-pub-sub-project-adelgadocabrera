package models;

/**
 * @author Alberto Delgado on 2/28/22
 * @project dsd-pub-sub
 * <p>
 * Record received by the consumer. Json file to be read
 */
public class ConsumerRecord<K, V> extends Record<K, V> {
    int offset;

    /**
     * Creates a consumer record from a producer record
     *
     * @param record
     * @param offset
     */
    public ConsumerRecord(ProducerRecord<K, V> record, int offset) {
        super(record.getTopic(), record.getKey(), record.getValue(), record.getTimestamp());
        this.offset = offset;
    }

    /**
     * Creates a consumer record
     *
     * @param topic
     * @param key
     * @param value
     * @param timestamp
     * @param offset
     */
    public ConsumerRecord(String topic, K key, V value, Long timestamp, int offset) {
        super(topic, key, value, timestamp);
        this.offset = offset;
    }

    /**
     * Offset getter
     *
     * @return
     */
    public int getOffset() {
        return offset;
    }

    /**
     * toString override for better readability
     *
     * @return
     */
    @Override
    public String toString() {
        return "Record: \n"
                + "     topic: " + topic + "\n"
                + "     key: " + key.toString() + "\n"
                + "     value: " + value.toString() + "\n"
                + "     timestamp: " + timestamp.toString() + "\n"
                + "     offset: " + offset;
    }
}
