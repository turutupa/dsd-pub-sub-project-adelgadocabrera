package utils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import models.BrokerConfig;
import models.ConsumerConfig;
import models.ProducerConfig;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;


/**
 * @author Alberto Delgado on 3/23/22
 * @project dsd-pub-sub
 * <p>
 * Reads the config settings. Config may contain settings for
 * - Broker
 * - Producer
 * - Consumer
 */
public class ConfigReader {
    /**
     * Reads specified file and creates a Config object
     *
     * @param file
     * @return
     */
    public static Config get(String file) {
        Config config = null;
        Gson gson = new Gson();
        try {
            config = gson.fromJson(new FileReader(file), Config.class);
        } catch (FileNotFoundException e) {
            System.err.println("Config file " + file + " not found: " + e.getMessage());
            System.exit(-1);
        }
        return config;

    }

    /**
     * Config file expected objects
     */
    public static class Config {
        @SerializedName("broker")
        public final BrokerConfig brokerConfig;

        @SerializedName("producer")
        public final ProducerConfig producerConfig;

        @SerializedName("consumers")
        public final List<ConsumerConfig> consumersConfig;

        @SerializedName("leader")
        public final BrokerConfig leaderConfig;

        Config(BrokerConfig brokerConfig,
               BrokerConfig leaderConfig,
               ProducerConfig producerConfig,
               List<ConsumerConfig> consumersConfig
        ) {
            this.brokerConfig = brokerConfig;
            this.leaderConfig = leaderConfig;
            this.producerConfig = producerConfig;
            this.consumersConfig = consumersConfig;
        }

        /**
         * For better readability
         *
         * @return
         */
        @Override
        public String toString() {
            String toString = "Config {" + "\n";
            if (brokerConfig != null) toString += brokerConfig + "\n";
            if (leaderConfig != null) toString += leaderConfig + "\n";
            if (producerConfig != null) toString += producerConfig + "\n";
            if (consumersConfig != null) {
                for (ConsumerConfig consumerConfig : consumersConfig)
                    toString += consumerConfig + "\n";
            }
            return toString;
        }
    }
}
