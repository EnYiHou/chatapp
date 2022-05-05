package protocol;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ListSerializer<T> implements Serializer<List<T>> {
    Serializer subSerializer;
    
    public ListSerializer(Serializer<T> subSerializer) {
        this.subSerializer = subSerializer;
    }
    
    @Override
    public byte[] serialize(List<T> list) throws ProtocolFormatException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        bytes.writeBytes(new IntegerSerializer().serialize(list.size()));
        for (T o : list)
            bytes.writeBytes(this.subSerializer.serialize(o));
        
        return bytes.toByteArray();
    }

    @Override
    public Deserialized<List<T>> deserialize(List<Byte> buf)
        throws ProtocolFormatException {
        Deserialized<Integer> size;
        List<T> list;
        int j = 0;
        
        size = new IntegerSerializer().deserialize(buf);
        
        list = new ArrayList<>(size.getValue());
        for (int i = 0; i < size.getValue(); ++i) {
            Deserialized<T> o = this.subSerializer.deserialize(
                buf.subList(j, buf.size())
            );
            
            j += o.getSize();
            
            list.add(o.getValue());
        }
        
        return new Deserialized<>(list, size.getSize() + j);
    }
}
