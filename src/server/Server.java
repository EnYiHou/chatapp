package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.stream.Stream;


public class Server {
    private ServerSocket sock;
    private LinkedHashMap<Socket, String> clients;
    private HashMap<String, Hash> userSecrets;
    public static final int PORT = 0xCAFE;
    private volatile boolean active;

    public Server() throws IOException {
        this.sock = new ServerSocket(PORT);
        this.clients = new LinkedHashMap<>();
        this.userSecrets = new HashMap<>();
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
    
    public void listen() {
        active = true;
        
        while (active) {
            
        }
    }
    
    public void closeConnection(Socket conn) throws IOException {
        if (this.clients.containsKey(conn)) {
            this.clients.remove(conn);
            conn.close();
        }
    }
    
    public void close() throws IOException {
        active = false;
        
        for (Socket conn : this.clients.keySet())
            this.closeConnection(conn);
        
        this.sock.close();
    }
}
