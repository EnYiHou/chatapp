package protocol;

import java.util.ArrayList;
import java.util.List;

public interface Serializer<T> {
    public byte[] serialize(T o) throws ProtocolFormatException;
    public Deserialized<T> deserialize(List<Byte> buf)
        throws ProtocolFormatException;
    
    default public Deserialized<T> deserialize(byte[] buf) throws ProtocolFormatException {
        final List<Byte> list = new ArrayList<>(buf.length);
        
        for (byte b : buf)
            list.add(b);
        
        return this.deserialize(list);
    }
}
