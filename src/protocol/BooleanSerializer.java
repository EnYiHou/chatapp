package protocol;

import java.util.List;

public class BooleanSerializer implements Serializer<Boolean> {
    @Override
    public byte[] serialize(Boolean o) throws ProtocolFormatException {
        return new byte[]{(byte)(o ? 1 : 0)};
    }

    @Override
    public Deserialized<Boolean> deserialize(List<Byte> buf) throws ProtocolFormatException {
        return new Deserialized<>(buf.get(0) != 0, 1);
    }
}
