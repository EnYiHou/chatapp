package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class ResponseSerializer implements Serializer<Response> {
    @Override
    public byte[] serialize(Response o) throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        
        builder.writeBytes(
            new EnumSerializer<>(EResponseType.class).serialize(o.getType())
        );
        
        builder.writeBytes(new BytesSerializer().serialize(o.getBody()));

        return builder.toByteArray();
    }

    @Override
    public Deserialized<Response> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<EResponseType> rawType =
            new EnumSerializer<>(EResponseType.class).deserialize(buf);
        
        Deserialized<byte[]> rawBody = new BytesSerializer().deserialize(
            buf.subList(rawType.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new Response(
                rawType.getValue(),
                rawBody.getValue()
            ),
            rawType.getSize() + rawBody.getSize()
        );
    }
    
}
