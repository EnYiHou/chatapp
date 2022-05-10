package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class SendFileAnnouncementSerializer implements Serializer<SendFileAnnouncement> {
    @Override
    public byte[] serialize(SendFileAnnouncement o) throws ProtocolFormatException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        
        final StringSerializer stringSerializer = new StringSerializer();
        
        builder.writeBytes(stringSerializer.serialize(o.getTransferCode()));
        builder.writeBytes(stringSerializer.serialize(o.getFileName()));
        builder.writeBytes(new LongSerializer().serialize(o.getSize()));
        
        return builder.toByteArray();
    }

    @Override
    public Deserialized<SendFileAnnouncement> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        final StringSerializer stringSerializer = new StringSerializer();
        
        Deserialized<String> rawTransferCode = stringSerializer.deserialize(buf);
        Deserialized<String> rawFileName = stringSerializer.deserialize(
            buf.subList(rawTransferCode.getSize(), buf.size())
        );
        
        Deserialized<Long> rawSize = new LongSerializer().deserialize(
            buf.subList(rawTransferCode.getSize() + rawFileName.getSize(), buf.size())
        );
        
        return new Deserialized<>(
            new SendFileAnnouncement(
                rawTransferCode.getValue(),
                rawFileName.getValue(),
                rawSize.getValue()
            ),
            rawTransferCode.getSize() + rawFileName.getSize() + rawSize.getSize()
        );
    }
}
