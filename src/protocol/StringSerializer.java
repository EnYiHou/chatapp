package protocol;

import java.nio.charset.Charset;
import java.util.List;

public class StringSerializer implements Serializer<String> {
    private Charset charset;
    
    public StringSerializer() throws ProtocolFormatException {
        this(Charset.defaultCharset());
    }
        
    public StringSerializer(Charset charset) throws ProtocolFormatException {
        this.charset = charset;
    }
    
    @Override
    public byte[] serialize(String o) throws ProtocolFormatException {
        return new BytesSerializer().serialize(o.getBytes(this.charset));
    }

    @Override
    public Deserialized<String> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<byte[]> raw = new BytesSerializer().deserialize(buf);
        
        return new Deserialized<>(
            new String(raw.getValue(), this.charset), raw.getSize()
        );
    }
    
}
