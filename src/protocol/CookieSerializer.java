package protocol;

import java.util.List;

public class CookieSerializer implements Serializer<Cookie> {
    private final BytesSerializer backend;
    
    public CookieSerializer() throws ProtocolFormatException {
        this.backend = new BytesSerializer();
    }
    
    @Override
    public byte[] serialize(Cookie o) throws ProtocolFormatException {
        return this.backend.serialize(o.getCookie());
    }

    @Override
    public Deserialized<Cookie> deserialize(List<Byte> buf) throws ProtocolFormatException {
        Deserialized<byte[]> rawDeserialization = this.backend.deserialize(buf);
        
        return new Deserialized<>(
            new Cookie(rawDeserialization.getValue()),
            rawDeserialization.getSize()
        );
    }
}
