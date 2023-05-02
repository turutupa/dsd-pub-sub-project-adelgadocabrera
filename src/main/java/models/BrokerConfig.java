package models;

/**
 * @author Alberto Delgado on 4/5/22
 * @project dsd-pub-sub
 * <p>
 * Broker config settings. Json file to be read
 */
public class BrokerConfig {
    public final int id;
    public final String hostname;
    public final int brokerPort;
    public final int zkPort;

    BrokerConfig(int id, String hostname, int brokerPort, int zkPort) {
        this.id = id;
        this.brokerPort = brokerPort;
        this.zkPort = zkPort;
        this.hostname = hostname;
    }

    /**
     * For better readability
     *
     * @return
     */
    @Override
    public String toString() {
        String config = "  BrokerConfig {" + "\n" +
                "   id=" + id + "\n" +
                "   broker.port=" + brokerPort + "\n" +
                "   zooKeeper.port=" + zkPort + "\n" +
                "   hostname=" + hostname + "\n";
        config += "  }" + "\n";
        return config;
    }
}
