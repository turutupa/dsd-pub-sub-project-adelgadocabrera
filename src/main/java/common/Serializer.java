package common;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * @author Alberto Delgado on 3/5/22
 * @project dsd-pub-sub
 * <p>
 * Serializer/Deserializer for every data type. For this project
 * we are only using Strings and Ints therefore only these 2 are
 * implemented. But if the project were to grow then it'd be useful
 * to add serializer for other data types such as long, double, short,
 * ByteBuffer, ByteArray etc.
 */
public abstract class Serializer<T> implements Serializeable<T> {
    public static Serializer<?> get(String type) {
        if (type.equals(SerializeableItems.STRING.name())) {
            return new StringSerializer();
        } else if (type.equals(SerializeableItems.INTEGER.name())) {
            return new IntegerSerializer();
        } else return null;
    }

    /**
     * Serializers are only created with static factory "get"
     */
    private Serializer() {
    }

    /**
     * String serializer/deserializer
     */
    public static class StringSerializer extends Serializer<String> {
        public byte[] serialize(String data) {
            return data.getBytes();
        }

        public String deserialize(byte[] data) {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    /**
     * Integer serializer/deserializer
     */
    private static class IntegerSerializer extends Serializer<Integer> {
        public byte[] serialize(Integer data) {
            return BigInteger.valueOf(data).toByteArray();
        }

        public Integer deserialize(byte[] data) {
            return new BigInteger(data).intValue();
        }
    }
}
