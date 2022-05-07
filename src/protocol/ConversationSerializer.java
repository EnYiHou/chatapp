package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class ConversationSerializer implements Serializer<Conversation> {
    @Override
    public byte[] serialize(Conversation o) throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        StringSerializer stringSerializer = new StringSerializer();
        
        builder.writeBytes(stringSerializer.serialize(o.getCode()));
        builder.writeBytes(stringSerializer.serialize(o.getName()));
        
        return builder.toByteArray();
    }

    @Override
    public Deserialized<Conversation> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        StringSerializer stringSerializer = new StringSerializer();
        
        Deserialized<String> rawCode = stringSerializer.deserialize(buf);
        Deserialized<String> rawName = stringSerializer.deserialize(
            buf.subList(rawCode.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new Conversation(
                rawCode.getValue(),
                rawName.getValue()
            ),
            rawCode.getSize() + rawName.getSize()
        );
    }
}
