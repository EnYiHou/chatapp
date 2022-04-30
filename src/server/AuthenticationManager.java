package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import protocol.Cookie;
import protocol.ProtocolFormatException;
import protocol.User;

public class AuthenticationManager {
    private final Set<UserSession> sessions;
    private final SecretManager secretManager;
    
    public AuthenticationManager() {
        this.secretManager = new SecretManager();
        this.sessions = ConcurrentHashMap.newKeySet();
    }
    
    public AuthenticationManager(File db)
        throws SecretFormatException, FileNotFoundException,
        SecretDuplicateException, NoSuchAlgorithmException {
        this.secretManager = new SecretManager(db);
        this.sessions = ConcurrentHashMap.newKeySet();
    }
    
    public UserSession login(String username, String password, Socket conn)
        throws AuthenticationFailureException, NoSuchAlgorithmException,
            ProtocolFormatException {

        if (!this.secretManager.verify(username, password))
            throw new AuthenticationFailureException("Incorrect credentials");
        
        UserSession sess = new UserSession(new User(username), conn);
        
        this.sessions.add(sess);
        
        return sess;
    }
    
    public UserSession signUp(String username, String password, Socket conn)
        throws AuthenticationFailureException, NoSuchAlgorithmException,
            ProtocolFormatException {

        if (!this.secretManager.create(username, password))
            throw new AuthenticationFailureException("User exists");
        
        UserSession sess = new UserSession(new User(username), conn);
        
        this.sessions.add(sess);
        
        return sess;
    }
    
    public UserSession loginCookie(Cookie cookie)
        throws AuthenticationFailureException {
        return this.sessions.stream()
            .filter((s) -> s.getCookie().equals(cookie))
            .findFirst()
            .orElseThrow(
                () -> new AuthenticationFailureException("Invalid cookie")
            );
    }
}
