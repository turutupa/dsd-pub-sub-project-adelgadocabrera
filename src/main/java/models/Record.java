package models;

/**
 * @author Alberto Delgado on 3/3/22
 * @project dsd-pub-sub
 * <p>
 * Base class for all records.
 */
public abstract class Record<K, V> {
    protected String topic;
    protected K key;
    protected V value;
    protected Long timestamp;

    /**
     * Without timestamp
     *
     * @param topic
     * @param key
     * @param value
     */
    public Record(String topic, K key, V value) {
        this(topic, key, value, null);
    }

    /**
     * With timestamp
     *
     * @param topic
     * @param key
     * @param value
     * @param timestamp
     */
    public Record(String topic, K key, V value, Long timestamp) {
        this.topic = topic;
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
    }

    /**
     * Topic getter
     *
     * @return
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Key getter
     *
     * @return
     */
    public K getKey() {
        return key;
    }

    /**
     * Value getter
     *
     * @return
     */
    public V getValue() {
        return value;
    }

    /**
     * Timestamp getter
     *
     * @return
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * toString for better readability
     *
     * @return
     */
    @Override
    public String toString() {
        return "Record: \n"
                + "     topic: " + topic + "\n"
                + "     key: " + key.toString() + "\n"
                + "     value: " + value.toString() + "\n"
                + "     timestamp: " + timestamp.toString();
    }
}
