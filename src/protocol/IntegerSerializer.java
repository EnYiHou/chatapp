package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class IntegerSerializer implements Serializer<Integer> {
    @Override
    public byte[] serialize(Integer o) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        for (int i = 0; i < Integer.BYTES; ++i)
            bytes.write((o >> (8 * i)) & 0xff);
        
        return bytes.toByteArray();
    }

    @Override
    public Deserialized<Integer> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Integer integer = 0;
        
        for (int i = 0; i < Integer.BYTES; ++i)
            integer |= (buf.get(i) & 0xff) << (8 * i);
        
        return new Deserialized<>(integer, Integer.BYTES);
    }
    
    public static int size() {
        return Integer.BYTES;
    }
}