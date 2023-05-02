package common;

/**
 * @author Alberto Delgado on 3/5/22
 * @project dsd-pub-sub
 * <p>
 * Record is serializeable if it can serialize and deserialize
 * the data
 */
public interface Serializeable<T> {
    byte[] serialize(T data);

    T deserialize(byte[] data);
}
