package protocol;

import java.util.List;

public class EnumSerializer<E extends Enum<E>> extends Serializer<E> {
    Class<E> enumClass;
    
    public EnumSerializer(Class<E> enumClass)
        throws ProtocolFormatException {
        super(new byte[]{'E', 'N', 'U', 'M'});
        
        this.enumClass = enumClass;
    }
    
    @Override
    protected byte[] onSerialize(E o) throws ProtocolFormatException {
        return new IntegerSerializer().serialize(o.ordinal());
    }

    @Override
    protected Deserialized<E> onDeserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<Integer> rawEnumValue =
            new IntegerSerializer().deserialize(buf);
        
        for (E constant : this.enumClass.getEnumConstants())
            if (rawEnumValue.getValue() == constant.ordinal())
                return new Deserialized<>(constant, rawEnumValue.getSize());
        
        throw new ProtocolFormatException("Enum value incompatible with enum");
    }
}