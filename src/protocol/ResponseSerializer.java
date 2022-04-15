package protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.List;

public class ResponseSerializer extends Serializer<Response> {
    private Charset charset;
    
    public ResponseSerializer() throws ProtocolFormatException {
        this(Charset.defaultCharset());
    }
    
    public ResponseSerializer(Charset charset) throws ProtocolFormatException {
        super(new byte[]{'R', 'E', 'S', 'P'});
        
        this.charset = charset;
    }
    
    @Override
    protected byte[] onSerialize(Response o) throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        
        builder.writeBytes(
            new EnumSerializer<>(EResponseType.class).serialize(o.getType())
        );
        
        builder.writeBytes(
            new BytesSerializer().serialize(
                o.getMessage().getBytes(this.charset)
            )
        );
        
        return builder.toByteArray();
    }

    @Override
    protected Deserialized<Response> onDeserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<EResponseType> rawResponseType =
            new EnumSerializer<>(EResponseType.class).deserialize(buf);
        
        Deserialized<byte[]> rawMessage = new BytesSerializer().deserialize(
            buf.subList(rawResponseType.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new Response(
                rawResponseType.getValue(),
                new String(rawMessage.getValue(), this.charset)
            ),
            rawResponseType.getSize() + rawMessage.getSize()
        );
    }
    
}
