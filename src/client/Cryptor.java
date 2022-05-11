package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Cryptor {
    private static final String CIPHER_TRANSFORM = "AES/GCM/NoPadding";
    private final Cipher cipher;
    private final byte[] salt;
    private SecretKey key;
    private static final int IV_LENGTH = 12;
    public static final int SALT_LENGTH = 16;

    private static SecretKey deriveKey(String password, byte[] salt)
        throws GeneralSecurityException {
        final SecretKeyFactory factory = SecretKeyFactory.getInstance(
            "PBKDF2WithHmacSHA256"
        );
        
        final KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            65536,
            256
        );
        
        return new SecretKeySpec(
            factory.generateSecret(spec).getEncoded(),
            "AES"
        );
    }
    
    private static byte[] getRandomBytes(int n) {
        byte[] buf = new byte[n];
        
        new SecureRandom().nextBytes(buf);
        
        return buf;
    }
    
    public Cryptor(String password) throws GeneralSecurityException {
        this(password, getRandomBytes(SALT_LENGTH));
    }
    
    public Cryptor(String password, byte[] salt) throws GeneralSecurityException {
        this.cipher = Cipher.getInstance(CIPHER_TRANSFORM);
        
        this.key = deriveKey(password, this.salt = salt);
    }
    
    public byte[] getSalt() {
        return this.salt;
    }
    
    public byte[] encrypt(byte[] data) throws GeneralSecurityException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        
        cipher.init(
            Cipher.ENCRYPT_MODE,
            this.key,
            new GCMParameterSpec(
                128,
                getRandomBytes(IV_LENGTH)
            )
        );
        
        builder.writeBytes(cipher.getIV());
        builder.writeBytes(cipher.doFinal(data));
        
        return builder.toByteArray();
    }
    
    public byte[] decrypt(byte[] data) throws GeneralSecurityException, IOException {
        final ByteArrayInputStream reader = new ByteArrayInputStream(data);
        
        cipher.init(
            Cipher.DECRYPT_MODE,
            this.key,
            new GCMParameterSpec(
                128,
                reader.readNBytes(IV_LENGTH)
            )
        );
        
        return cipher.doFinal(reader.readAllBytes());
    }
}
