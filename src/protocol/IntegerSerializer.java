package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class IntegerSerializer extends Serializer<Integer> {
    private static final byte[] localHeader =
        String.format("SI%02d", Integer.SIZE).getBytes();
    
    public IntegerSerializer() throws ProtocolFormatException {
        super(localHeader);
    }
    
    @Override
    protected byte[] onSerialize(Integer o) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        for (int i = 0; i < Integer.BYTES; ++i)
            bytes.write((o >> (8 * i)) & 0xff);
        
        return bytes.toByteArray();
    }

    @Override
    protected Deserialized<Integer> onDeserialize(List<Byte> buf)
        throws ProtocolFormatException{
        Integer integer = 0;
        
        for (int i = 0; i < Integer.BYTES; ++i)
            integer |= buf.get(i) << (8 * i);
        
        return new Deserialized<>(integer, Integer.BYTES);
    }
    
    public static int size() {
        return localHeader.length + Integer.BYTES;
    }
}
