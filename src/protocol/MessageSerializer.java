package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MessageSerializer implements Serializer<Message> {
    @Override
    public byte[] serialize(Message o) throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        
        builder.writeBytes(new StringSerializer().serialize(o.getAuthor()));
        builder.writeBytes(new BytesSerializer().serialize(o.getMessage()));
        builder.writeBytes(new LongSerializer().serialize(o.getTimestamp()));
        
        return builder.toByteArray();
    }

    @Override
    public Deserialized<Message> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<String> rawAuthor = new StringSerializer().deserialize(
            buf
        );
        
        Deserialized<byte[]> rawMessage = new BytesSerializer().deserialize(
            buf.subList(rawAuthor.getSize(), buf.size())
        );
        
        Deserialized<Long> rawTimestamp = new LongSerializer().deserialize(
            buf.subList(rawAuthor.getSize() + rawMessage.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new Message(
                rawMessage.getValue(),
                rawAuthor.getValue(),
                rawTimestamp.getValue()
            ),
            rawAuthor.getSize() + rawMessage.getSize() + rawTimestamp.getSize()
        );
    }
}
