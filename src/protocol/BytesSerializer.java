package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class BytesSerializer extends Serializer<byte[]>{
    public BytesSerializer() throws ProtocolFormatException {
        super(new byte[]{'B', 'Y', 'T', 'E'});
    }
    
    @Override
    protected byte[] onSerialize(byte[] o) throws ProtocolFormatException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        bytes.writeBytes(new IntegerSerializer().serialize(o.length));
        bytes.writeBytes(o);
        
        return bytes.toByteArray();
    }
    
    @Override
    protected Deserialized<byte[]> onDeserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<Integer> size;
        byte[] stringByteArray;
        
        size = new IntegerSerializer().deserialize(buf);
        
        stringByteArray = new byte[size.getValue()];
        for (int i = 0; i < size.getValue(); ++i)
            stringByteArray[i] = buf.get(size.getSize() + i);
        
        return new Deserialized<>(stringByteArray, size.getSize() + size.getValue());
    }
}
