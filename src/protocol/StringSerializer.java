package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class StringSerializer {
    private final static byte[] header = {'S', 'T', 'R', 'N'};
    
    public static byte[] serialize(String o) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        bytes.writeBytes(header);
        bytes.writeBytes(new IntegerSerializer().serialize(o.length()));
        bytes.writeBytes(o.getBytes());
        
        return bytes.toByteArray();
    }

    public static String deserialize(List<Byte> buf) throws ProtocolFormatException {
        int i, size;
        byte[] stringByteArray;
        
        for (i = 0; i < header.length; ++i)
            if (header[i] != buf.get(i))
                throw new ProtocolFormatException(String.class, header);
        
        size = new IntegerSerializer().deserialize(buf.subList(i, buf.size()));
        
        stringByteArray = new byte[size];
        for (int j = 0; j < size; ++j)
            stringByteArray[j] = buf.get(i + j);
        
        return new String(stringByteArray);
    }
}
