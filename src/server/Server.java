package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.stream.Stream;
import protocol.Cookie;


public class Server {
    private LinkedHashMap<Cookie, String> sessions;
    private HashMap<String, Hash> userSecrets;
    public static final int PORT = 5200;
    private ServerListenerThread listener;

    public Server() throws IOException {
        this.sessions = new LinkedHashMap<>();
        this.userSecrets = new HashMap<>();
        this.listener = null;
    }
    
    public Server(File db) throws SecretFormatException, SecretDuplicateException, HashInvalidLengthException, IOException, NoSuchAlgorithmException {
        this();
        
        Scanner sc = new Scanner(db);
        
        Stream<MatchResult> matches = sc.findAll("(\\w+):([A-Fa-f0-9]{64})");
        
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
        
        sc.close();
    }
    
    public void saveSecrets(String path) throws IOException {
        FileWriter file = new FileWriter(path);
        
        for (String key : this.userSecrets.keySet())
            file.write(String.format(
                "%s:%s%n", key, this.userSecrets.get(key).hex()
            ));
        
        file.close();
    }
    
    public void listen() throws IOException {
        if (this.listener != null && this.listener.isAlive())
            throw new IOException("Server already listening");
        
        this.listener = new ServerListenerThread(PORT, this);
        this.listener.start();
    }
    
    public boolean isListening() {
        return this.listener.isAlive();
    }
    
    public void close() throws IOException, InterruptedException {
        if (this.listener == null || !this.listener.isAlive())
            throw new IOException("Server not listening");
        
        this.listener.interrupt();
        this.listener.join();
    }
}
