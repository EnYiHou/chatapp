package protocol;

import java.util.List;

public interface ISerializer<T> {
    public byte[] serialize(T o);
    public T deserialize(List<Byte> buf) throws ProtocolFormatException;
}
