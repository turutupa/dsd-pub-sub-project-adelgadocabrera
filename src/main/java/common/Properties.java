package common;

/**
 * @author Alberto Delgado on 3/5/22
 * @project dsd-pub-sub
 * <p>
 * Properties to be set for Consumers/Producers
 * Most important ones would be to (1) specify Broker hostname and port
 * (2) key/value serializer/deserializer, (3) consumer method (push/pull)
 * if consumer, (optional) for consumers you may specify the starting offset
 */
public class Properties {
    // made public static constants in case devs want to import these
    // instead of hard-coding strings.
    public static final String ID = "id";
    public static final String PORT = "port";
    public static final String LOCAL_PORT = "local.port";
    public static final String HOSTNAME = "hostname";
    public static final String KEY_SERIALIZER = "key.serializer";
    public static final String VALUE_SERIALIZER = "value.serializer";
    public static final String KEY_DESERIALIZER = "key.deserializer";
    public static final String VALUE_DESERIALIZER = "value.deserializer";
    public static final String POLL_METHOD_CONSUMER = "poll.method.consumer";
    public static final String POLL_CONSUMER = "poll.consumer";
    public static final String PUSH_CONSUMER = "push.consumer";
    public static final String OFFSET_CONSUMER = "offset.consumer";
    public static final String INTERNAL_TIMEOUT = "timeout.consumer";

    // class private properties
    private int id;
    private int port;
    private int localPort;
    private String hostname;
    private Serializer<?> keySerializer;
    private Serializer<?> valueSerializer;
    private Serializer<?> keyDeserializer;
    private Serializer<?> valueDeserializer;
    private String consumerMethod;
    private int consumerOffset = -1; // by default no records have been received
    private int timeout = 50; // by default 50ms for data polling

    public Properties() {
    }

    /**
     * Allows adding properties. All properties must be strings. If an int is
     * expected it will be parsed, but all expected properties are expected
     * to be strings.
     *
     * @param propName
     * @param prop
     */
    public void put(String propName, String prop) {
        switch (propName) {
            case ID -> id = Integer.parseInt(prop);
            case HOSTNAME -> hostname = prop;
            case PORT -> port = Integer.parseInt(prop);
            case LOCAL_PORT -> localPort = Integer.parseInt(prop);
            case KEY_SERIALIZER -> keySerializer = Serializer.get(prop);
            case VALUE_SERIALIZER -> valueSerializer = Serializer.get(prop);
            case KEY_DESERIALIZER -> keyDeserializer = Serializer.get(prop);
            case VALUE_DESERIALIZER -> valueDeserializer = Serializer.get(prop);
            case POLL_METHOD_CONSUMER -> consumerMethod = prop;
            case OFFSET_CONSUMER -> consumerOffset = Integer.parseInt(prop);
            case INTERNAL_TIMEOUT -> timeout = Integer.parseInt(prop);
            default -> {
                System.out.println("Unexpected property " + propName + ".");
            }
        }
    }

    /**
     * Id getter
     */
    public int getId() {
        return id;
    }

    /**
     * Hostname getter
     *
     * @return
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Port getter
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * Port running locally
     *
     * @return
     */
    public int getLocalPort() {
        return localPort;
    }

    /**
     * Key Serializer getter
     *
     * @return
     */
    public Serializer<?> getKeySerializer() {
        return keySerializer;
    }

    /**
     * Value Serializer getter
     *
     * @return
     */
    public Serializer<?> getValueSerializer() {
        return valueSerializer;
    }

    /**
     * Key Deserializer getter
     *
     * @return
     */
    public Serializer<?> getKeyDeserializer() {
        return keyDeserializer;
    }

    /**
     * Value Deserializer getter
     *
     * @return
     */
    public Serializer<?> getValueDeserializer() {
        return valueDeserializer;
    }

    /**
     * Consumer method getter
     *
     * @return
     */
    public String getConsumerMethod() {
        return consumerMethod;
    }

    /**
     * Consumer offset getter
     *
     * @return
     */
    public int getConsumerOffset() {
        return consumerOffset;
    }

    /**
     * Timeout getter
     *
     * @return
     */
    public int getTimeout() {
        return timeout;
    }
}
