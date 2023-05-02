package models;

import java.util.List;

/**
 * @author Alberto Delgado on 4/5/22
 * @project dsd-pub-sub
 * <p>
 * Producer config settings. To read from json file
 */
public class ProducerConfig {
    public final int id;
    public final int port;
    public final String hostname;
    public final List<String> topics;
    public final String keySerializer;
    public final String valueSerializer;
    public final int localPort;

    ProducerConfig(
            int id,
            int port,
            int localPort,
            String hostname,
            List<String> topics,
            String keySerializer,
            String valueSerializer
    ) {
        this.id = id;
        this.port = port;
        this.localPort = localPort;
        this.hostname = hostname;
        this.topics = topics;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    /**
     * For better readability
     *
     * @return
     */
    @Override
    public String toString() {
        return "  ProducerConfig {" + "\n" +
                "   port=" + port + "\n" +
                "   local.port=" + localPort + "\n" +
                "   hostname='" + hostname + '\'' + "\n" +
                "   topics=" + topics + "\n" +
                "   key.serializer='" + keySerializer + '\'' + "\n" +
                "   value.serializer='" + valueSerializer + '\'' + "\n" +
                "  }" + "\n";
    }
}
