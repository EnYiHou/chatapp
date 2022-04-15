package server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Hash {
    public static final String HASH_ALGO = "SHA-1";
    private MessageDigest digest;
    private byte[] hash;
    
    private Hash() throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance(HASH_ALGO);
        this.hash = null;
    }
    
    public Hash(String value) throws NoSuchAlgorithmException {
        this();
        
        this.hash = digest.digest(value.getBytes());
    }
    
    public static Hash from(String hexHash) throws HashInvalidLengthException, NoSuchAlgorithmException {
        Hash hash = new Hash();
        
        if (hexHash.length() != hash.digest.getDigestLength() * 2)
            throw new HashInvalidLengthException();
        
        hash.hash = new byte[hash.digest.getDigestLength()];
        
        for (int i = 0; i < hash.hash.length; ++i)
            hash.hash[i] = Byte.parseByte(hexHash.substring(i * 2, i * 2 + 2));
        
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
            
        if (obj == null)
            return false;
        
        if (getClass() != obj.getClass())
            return false;
        
        final Hash other = (Hash)obj;
        
        return Arrays.equals(this.hash, other.hash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.hash);
    }

    public String hex() {
        StringBuilder builder = new StringBuilder();
        final char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        
        for (byte b : this.hash) {
            builder.append(hexChars[(b & 0xF0) >>> 4]);
            builder.append(hexChars[b & 0xF]);
        }
        
        return builder.toString();
    }
}
