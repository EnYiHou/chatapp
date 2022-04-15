package protocol;

import java.util.List;

public class CookieSerializer extends Serializer<Cookie> {
    private final BytesSerializer backend;
    
    public CookieSerializer() throws ProtocolFormatException {
        super(new byte[]{'C', 'O', 'O', 'K'});
        this.backend = new BytesSerializer();
    }
    
    @Override
    protected byte[] onSerialize(Cookie o) throws ProtocolFormatException {
        return this.backend.serialize(o.getCookie());
    }

    @Override
    protected Deserialized<Cookie> onDeserialize(List<Byte> buf) throws ProtocolFormatException {
        Deserialized<byte[]> rawDeserialization = this.backend.deserialize(buf);
        
        return new Deserialized<>(new Cookie(rawDeserialization.getValue()), rawDeserialization.getSize());
    }
}
