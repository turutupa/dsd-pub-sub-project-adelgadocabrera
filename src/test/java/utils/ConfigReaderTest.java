package utils;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/**
 * @author Alberto Delgado on 3/24/22
 * @project dsd-pub-sub
 */
public class ConfigReaderTest {

    @Test
    @DisplayName("should read broker info")
    public void broker() {
        ConfigReader.Config config = ConfigReader.get("config.json");
        Assertions.assertEquals(config.brokerConfig.brokerPort, 5000);
    }

    @Test
    @DisplayName("should read consumer info")
    public void consumer() {
        ConfigReader.Config config = ConfigReader.get("config.json");
        Assertions.assertEquals(config.consumersConfig.get(0).port, 5000);
        Assertions.assertEquals(config.consumersConfig.get(0).valueDeserializer, "STRING");
        Assertions.assertEquals(config.consumersConfig.get(0).keyDeserializer, "STRING");
    }

    @Test
    @DisplayName("should read producer info")
    public void producer() {
        ConfigReader.Config config = ConfigReader.get("config.json");
        Assertions.assertEquals(config.producerConfig.port, 5000);
        Assertions.assertEquals(config.producerConfig.valueSerializer, "STRING");
        Assertions.assertEquals(config.producerConfig.keySerializer, "STRING");
    }
}