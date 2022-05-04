package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class ArraySerializer<T> implements Serializer<T[]> {
    Serializer subSerializer;
    
    public ArraySerializer(Serializer<T> subSerializer) {
        this.subSerializer = subSerializer;
    }
    
    @Override
    public byte[] serialize(T[] arr) throws ProtocolFormatException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        bytes.writeBytes(new IntegerSerializer().serialize(arr.length));
        for (T o : arr)
            bytes.writeBytes(this.subSerializer.serialize(o));
        
        return bytes.toByteArray();
    }

    @Override
    public Deserialized<T[]> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<Integer> size;
        T[] arr;
        int j = 0;
        
        size = new IntegerSerializer().deserialize(buf);
        
        arr = (T[])new Object[size.getValue()];
        for (int i = 0; i < size.getValue(); ++i) {
            Deserialized<T> o = this.subSerializer.deserialize(
                buf.subList(j, buf.size())
            );
            
            j += o.getSize();
            
            arr[i] = o.getValue();
        }
        
        return new Deserialized<>(arr, size.getSize() + j);
    }
}
