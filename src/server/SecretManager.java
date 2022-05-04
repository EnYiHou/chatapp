package server;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class SecretManager {
    private ConcurrentHashMap<String, Hash> userSecrets;
    private final DatabaseManager dbManager;
    
    public SecretManager(DatabaseManager dbManager)
        throws SecretFormatException, FileNotFoundException,
        SecretDuplicateException, NoSuchAlgorithmException {
        this.userSecrets = new ConcurrentHashMap<>();
        this.dbManager = dbManager;
    }
    
    public boolean verify(String username, String password)
        throws NoSuchAlgorithmException {
        return new Hash(password).equals(this.userSecrets.get(username));
    }
    
    public boolean create(String username, String password)
        throws NoSuchAlgorithmException {
        
        if (this.userSecrets.containsKey(username))
            return false;
        
        this.userSecrets.put(username, new Hash(password));
        
        return true;
    }
    
    public void save(String path) throws IOException {
        try (FileWriter file = new FileWriter(path)) {
            for (String key : this.userSecrets.keySet())
                file.write(String.format(
                        "%s:%s%n", key, this.userSecrets.get(key).hex()
                ));
        }
    }
}
