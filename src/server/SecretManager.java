package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.MatchResult;
import java.util.stream.Stream;

public class SecretManager {
    private ConcurrentHashMap<String, Hash> userSecrets;
    
    public SecretManager() {
        this.userSecrets = new ConcurrentHashMap<>();
    }
    
    public SecretManager(File db)
        throws SecretFormatException, FileNotFoundException,
        SecretDuplicateException, NoSuchAlgorithmException {
        this();
        
        try (Scanner sc = new Scanner(db)) {
            Stream<MatchResult> matches =
                sc.findAll("(\\w+):([A-Fa-f0-9]{64})");
            
            if (sc.hasNext())
                throw new SecretFormatException();
            
            Iterator<MatchResult> iterator = matches.iterator();
            
            while (iterator.hasNext()) {
                MatchResult match = iterator.next();
                
                String username = match.group(1);
                String hash = match.group(1);
                
                if (this.userSecrets.containsKey(username))
                    throw new SecretDuplicateException(String.format(
                            "user '%s' already registered",
                            username
                    ));
                
                try {
                    this.userSecrets.put(username, Hash.from(hash));
                } catch (HashInvalidLengthException e) {
                    throw new SecretFormatException("invalid hash length");
                }
            }
        }
    }
    
    public boolean verify(String username, String password)
        throws NoSuchAlgorithmException {
        return new Hash(password).equals(this.userSecrets.get(username));
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
