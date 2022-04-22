package protocol;

import java.nio.charset.Charset;
import java.util.List;

public class StringSerializer extends Serializer<String> {
    private Charset charset;
    
    public StringSerializer() throws ProtocolFormatException {
        this(Charset.defaultCharset());
    }
        
    public StringSerializer(Charset charset) throws ProtocolFormatException {
        super(new byte[]{'S', 'T', 'R', 'N'});
        
        this.charset = charset;
    }
    
    @Override
    protected byte[] onSerialize(String o) throws ProtocolFormatException {
        return new BytesSerializer().serialize(o.getBytes(this.charset));
    }

    @Override
    protected Deserialized<String> onDeserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<byte[]> raw = new BytesSerializer().deserialize(buf);
        
        return new Deserialized<>(
            new String(raw.getValue(), this.charset), raw.getSize()
        );
    }
    
}
