package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class LongSerializer implements Serializer<Long> {
    @Override
    public byte[] serialize(Long o) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        for (int i = 0; i < Long.BYTES; ++i)
            bytes.write((byte)((o >> (8 * i)) & 0xff));
        
        return bytes.toByteArray();
    }

    @Override
    public Deserialized<Long> deserialize(List<Byte> buf)
        throws ProtocolFormatException{
        long value = 0;

        for (int i = 0; i < Long.BYTES; ++i)
            value |= (long)(buf.get(i) & 0xff) << (8 * i);
        
        return new Deserialized<>(value, Long.BYTES);
    }
    
    public static int size() {
        return Long.BYTES;
    }
}
