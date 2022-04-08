package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class IntegerSerializer implements ISerializer<Integer> {
    private static final byte[] header = String.format("INT%d", Integer.SIZE).getBytes();
    
    @Override
    public byte[] serialize(Integer o) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        bytes.writeBytes(header);
        
        for (int i = 0; i < Integer.BYTES; ++i)
            bytes.write((o >> (8 * i)) & 0xff);
        
        return bytes.toByteArray();
    }

    @Override
    public Integer deserialize(List<Byte> buf) throws ProtocolFormatException{
        int i;
        Integer integer = 0;
        
        for (i = 0; i < header.length; ++i)
            if (header[i] != buf.get(i))
                throw new ProtocolFormatException(Integer.class, header);
        
        for (int j = i; j < Integer.BYTES; ++j)
            integer |= buf.get(i + j) << (8 * j);
        
        return integer;
    }
}
