package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class LoginBodySerializer extends Serializer<LoginBody> {
    public LoginBodySerializer() throws ProtocolFormatException {
        super(new byte[]{'A', 'U', 'T', 'H'});
    }
    
    @Override
    protected byte[] onSerialize(LoginBody o)
        throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        StringSerializer serializer = new StringSerializer();
        
        builder.writeBytes(serializer.serialize(o.getUsername()));
        builder.writeBytes(serializer.serialize(o.getPassword()));
        
        return builder.toByteArray();
    }

    @Override
    protected Deserialized<LoginBody> onDeserialize(List<Byte> buf)
        throws ProtocolFormatException {
        StringSerializer serializer = new StringSerializer();
        
        Deserialized<String> rawUsername = serializer.deserialize(buf);
        
        Deserialized<String> rawPassword = serializer.deserialize(
            buf.subList(rawUsername.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new LoginBody(rawUsername.getValue(), rawPassword.getValue()),
            rawUsername.getSize() + rawPassword.getSize()
        );
    }
}
