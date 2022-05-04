package protocol;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;

public class Cookie {
    private static final SecureRandom rng = new SecureRandom();
    private final byte cookie[];
    private static final int COOKIE_SIZE = 32;
    
    public static Cookie emptyCookie() {
        return new Cookie(new byte[0]);
    }
    
    public Cookie() {
        ByteArrayOutputStream cookieGenerator = new ByteArrayOutputStream();
        
        rng.ints(COOKIE_SIZE, Byte.MIN_VALUE, Byte.MAX_VALUE + 1).forEach(
            (i) -> cookieGenerator.write(i)
        );
        
        this.cookie = cookieGenerator.toByteArray();
    }
    
    public Cookie(byte[] cookie) {
        this.cookie = cookie;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass())
            return false;
        
        Cookie o = (Cookie)obj;
        
        return Arrays.equals(this.cookie, o.cookie);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.cookie);
    }
    
    public final byte[] getCookie() {
        return this.cookie;
    }
}
