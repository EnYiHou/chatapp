package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class RequestSerializer implements Serializer<Request> {
    @Override
    public byte[] serialize(Request o) throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        
        builder.writeBytes(
            new EnumSerializer<>(ERequestType.class).serialize(o.getType())
        );
        
        builder.writeBytes(new CookieSerializer().serialize(o.getCookie()));
        
        builder.writeBytes(new BytesSerializer().serialize(o.getBody()));
        
        return builder.toByteArray();
    }

    @Override
    public Deserialized<Request> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<ERequestType> rawType =
            new EnumSerializer<>(ERequestType.class).deserialize(buf);
        
        Deserialized<Cookie> rawCookie = new CookieSerializer().deserialize(
            buf.subList(rawType.getSize(), buf.size())
        );
        
        Deserialized<byte[]> rawBody = new BytesSerializer().deserialize(
            buf.subList(rawType.getSize() + rawCookie.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new Request(
                rawType.getValue(),
                rawCookie.getValue(),
                rawBody.getValue()
            ),
            rawType.getSize() + rawCookie.getSize() + rawBody.getSize()
        );
    }
}
