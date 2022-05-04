package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class CredentialsBodySerializer implements Serializer<CredentialsBody> {
    @Override
    public byte[] serialize(CredentialsBody o)
        throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        StringSerializer serializer = new StringSerializer();
        
        builder.writeBytes(serializer.serialize(o.getUsername()));
        builder.writeBytes(serializer.serialize(o.getPassword()));
        
        return builder.toByteArray();
    }

    @Override
    public Deserialized<CredentialsBody> deserialize(List<Byte> buf)
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
