package server;

import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import protocol.Cookie;
import protocol.ProtocolFormatException;
import protocol.User;
import java.sql.Connection;
import java.sql.SQLException;

public class AuthenticationManager {
    private final Set<UserSession> sessions;
    private final SecretManager secretManager;
    
    public AuthenticationManager(SecretManager secretManager) {
        this.sessions = ConcurrentHashMap.newKeySet();
        this.secretManager = secretManager;
    }
    
    public UserSession login(String username, String password, Socket conn)
        throws AuthenticationFailureException, NoSuchAlgorithmException,
            ProtocolFormatException, SQLException {

        User user = new User(username);
        
        if (!this.secretManager.verify(username, password))
            throw new AuthenticationFailureException("Incorrect credentials");
        
        UserSession sess = new UserSession(user, conn);
        
        this.sessions.add(sess);
        
        return sess;
    }
    
    public UserSession signUp(String username, String password, Socket conn)
        throws AuthenticationFailureException, NoSuchAlgorithmException,
            ProtocolFormatException, SQLException {

        User user = new User(username);
        
        if (!this.secretManager.create(username, password))
            throw new AuthenticationFailureException("User exists");
        
        UserSession sess = new UserSession(user, conn);
        
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
    
    public void logoutSession(UserSession session) throws AuthenticationFailureException {
        if (!this.sessions.remove(session))
            throw new AuthenticationFailureException("Invalid session");
    }
}
