package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class FileAnnouncementSerializer implements Serializer<FileAnnouncement> {
    @Override
    public byte[] serialize(FileAnnouncement o) throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        
        final StringSerializer stringSerializer = new StringSerializer();
        
        builder.writeBytes(stringSerializer.serialize(o.getSender()));
        builder.writeBytes(stringSerializer.serialize(o.getFileName()));
        builder.writeBytes(new LongSerializer().serialize(o.getSize()));
        
        return builder.toByteArray();
    }

    @Override
    public Deserialized<FileAnnouncement> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        final StringSerializer stringSerializer = new StringSerializer();
        
        Deserialized<String> rawSender = stringSerializer.deserialize(buf);
        Deserialized<String> rawFileName = stringSerializer.deserialize(
            buf.subList(rawSender.getSize(), buf.size())
        );
        
        Deserialized<Long> rawSize = new LongSerializer().deserialize(
            buf.subList(rawSender.getSize() + rawFileName.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new FileAnnouncement(
                rawSender.getValue(),
                rawFileName.getValue(),
                rawSize.getValue()
            ),
            rawSender.getSize() + rawFileName.getSize() + rawSize.getSize()
        );
    }
}
