package protocol;

import java.io.ByteArrayOutputStream;
import java.util.List;


public class CookieSerializer implements ISerializer<Cookie> {
    private static byte header[] = {'C', 'O', 'O', 'K'};
    
    @Override
    public byte[] serialize(Cookie o) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        bytes.writeBytes(header);
        bytes.writeBytes(o.getCookie());
        
        return bytes.toByteArray();
    }

    @Override
    public Cookie deserialize(List<Byte> buf) throws ProtocolFormatException {
        byte[] bytes;
        
        bytes = new byte[buf.size()];
        for (int i = 0; i < buf.size(); ++i)
            bytes[i] = buf.get(i);
        
        return new Cookie(bytes);
    }
}
