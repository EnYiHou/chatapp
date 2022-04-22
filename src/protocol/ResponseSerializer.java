package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class ResponseSerializer extends Serializer<Response> {
    public ResponseSerializer() throws ProtocolFormatException {
        super(new byte[]{'R', 'E', 'S', 'P'});
    }
    
    @Override
    protected byte[] onSerialize(Response o) throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        
        builder.writeBytes(
            new EnumSerializer<>(EResponseType.class).serialize(o.getType())
        );
        
        builder.writeBytes(new BytesSerializer().serialize(o.getBody()));
        
        return builder.toByteArray();
    }

    @Override
    protected Deserialized<Response> onDeserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<EResponseType> rawResponseType =
            new EnumSerializer<>(EResponseType.class).deserialize(buf);
        
        Deserialized<byte[]> rawBody = new BytesSerializer().deserialize(
            buf.subList(rawResponseType.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new Response(
                rawResponseType.getValue(),
                rawBody.getValue()
            ),
            rawResponseType.getSize() + rawBody.getSize()
        );
    }
    
}
