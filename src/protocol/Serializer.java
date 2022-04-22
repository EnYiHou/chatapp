package protocol;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class Serializer<T> {
    private final byte[] header;
    private final static int HEADER_SIZE = 4;

    protected Serializer(byte[] header) throws ProtocolFormatException {
        if (header.length != HEADER_SIZE)
            throw new ProtocolFormatException("Invalid header");
        
        this.header = header;
    }
    
    public final byte[] serialize(T o) throws ProtocolFormatException {
        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        
        serialized.writeBytes(this.header);
        serialized.writeBytes(this.onSerialize(o));

        return serialized.toByteArray();
    }
    
    public final Deserialized<T> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        if (buf.size() < this.header.length)
            throw new ProtocolFormatException("Invalid length");
        
        for (int i = 0; i < this.header.length; ++i)
            if (this.header[i] != buf.get(i))
                throw new ProtocolFormatException(
                    this.getClass().getGenericSuperclass().getClass(),
                    this.header
                );
        
        Deserialized<T> deserialized = this.onDeserialize(
            buf.subList(this.header.length, buf.size())
        );
        
        return new Deserialized<>(
            deserialized.getValue(), this.header.length + deserialized.getSize()
        );
    }
    
    public final Deserialized<T> deserialize(byte[] buf)
        throws ProtocolFormatException {
        final List<Byte> list = new ArrayList<>(buf.length);
        
        for (byte b : buf)
            list.add(b);
        
        return this.deserialize(list);
    }
    
    abstract protected byte[] onSerialize(T o) throws ProtocolFormatException;
    abstract protected Deserialized<T> onDeserialize(List<Byte> buf)
        throws ProtocolFormatException;
}
