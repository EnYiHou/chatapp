package protocol;

public interface Serializable<T> {
    public byte[] serialize(T o);
    public T deserialize(byte[] s);
}
