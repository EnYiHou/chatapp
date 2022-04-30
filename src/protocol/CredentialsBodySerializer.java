package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class CredentialsBodySerializer extends Serializer<CredentialsBody> {
    public CredentialsBodySerializer() throws ProtocolFormatException {
        super(new byte[]{'C', 'R', 'E', 'D'});
    }
    
    @Override
    protected byte[] onSerialize(CredentialsBody o)
        throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        StringSerializer serializer = new StringSerializer();
        
        builder.writeBytes(serializer.serialize(o.getUsername()));
        builder.writeBytes(serializer.serialize(o.getPassword()));
        
        return builder.toByteArray();
    }

    @Override
    protected Deserialized<CredentialsBody> onDeserialize(List<Byte> buf)
        throws ProtocolFormatException {
        StringSerializer serializer = new StringSerializer();
        
        Deserialized<String> rawUsername = serializer.deserialize(buf);
        
        Deserialized<String> rawPassword = serializer.deserialize(
            buf.subList(rawUsername.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new CredentialsBody(rawUsername.getValue(), rawPassword.getValue()),
            rawUsername.getSize() + rawPassword.getSize()
        );
    }
}
