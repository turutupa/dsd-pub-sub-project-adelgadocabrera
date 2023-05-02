package models;

import java.util.List;

/**
 * @author Alberto Delgado on 4/5/22
 * @project dsd-pub-sub
 * <p>
 * Consumer config settings. Json file to be read
 */

public class ConsumerConfig {
    public final int id;
    public final int port;
    public final int localPort;
    public final String hostname;
    public final List<String> topics;
    public final String keyDeserializer;
    public final String valueDeserializer;
    public final String pollMethodConsumer;
    public final int timeout;
    public final int offset;

    ConsumerConfig(
            int id,
            int port,
            int localPort,
            String hostname,
            List<String> topics,
            String keyDeserializer,
            String valueDeserializer,
            String pollMethodConsumer,
            int timeout,
            int offset
    ) {
        this.id = id;
        this.port = port;
        this.localPort = localPort;
        this.hostname = hostname;
        this.topics = topics;
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;
        this.pollMethodConsumer = pollMethodConsumer;
        this.timeout = timeout;
        this.offset = offset;
    }

    /**
     * For better readability
     *
     * @return
     */
    @Override
    public String toString() {
        return "  ConsumerConfig {" + "\n" +
                "   port=" + port + "\n" +
                "   hostname='" + hostname + '\'' + "\n" +
                "   topics=" + topics + "\n" +
                "   key.deserializer='" + keyDeserializer + '\'' + "\n" +
                "   value.deserializer='" + valueDeserializer + '\'' + "\n" +
                "   poll.method.consumer='" + pollMethodConsumer + '\'' + "\n" +
                "   consumer.timeout=" + timeout + "\n" +
                "   offset.timeout=" + offset + "\n" +
                "  }" + "\n";
    }
}
