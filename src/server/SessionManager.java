package server;

import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import protocol.Cookie;
import protocol.ProtocolFormatException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SessionManager {
    private final Set<UserSession> sessions;
    private final SecretManager secretManager;
    private final HashMap<String, Socket> transferCodes;
    private final static int CODE_LENGTH = 8;
    private final static char[] CODE_SET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        .toCharArray();

    public SessionManager(SecretManager secretManager) {
        this.sessions = ConcurrentHashMap.newKeySet();
        this.secretManager = secretManager;
        this.transferCodes = new HashMap<>();
    }
    
    public UserSession login(String username, String password, Socket conn)
        throws AuthenticationFailureException, NoSuchAlgorithmException,
            ProtocolFormatException, SQLException {

        if (!this.secretManager.verify(username, password))
            throw new AuthenticationFailureException("Incorrect credentials");
        
        UserSession sess = new UserSession(username, conn);
        
        this.sessions.add(sess);
        
        return sess;
    }
    
    public UserSession signUp(String username, String password, Socket conn)
        throws AuthenticationFailureException, NoSuchAlgorithmException,
            ProtocolFormatException, SQLException {

        if (!this.secretManager.create(username, password))
            throw new AuthenticationFailureException("User exists");
        
        UserSession sess = new UserSession(username, conn);
        
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
    
    public void logoutSession(UserSession session)
        throws AuthenticationFailureException {
        if (!this.sessions.remove(session))
            throw new AuthenticationFailureException("Invalid session");
    }
    
    public List<UserSession> getSessionsForConversation(int conversationId) {
        this.reap();
        
        return this.sessions.stream()
            .filter(s -> s.getConversation() == conversationId)
            .collect(Collectors.toList());
    }
    
    private void reap() {
        this.sessions.removeIf(UserSession::isDead);
    }
    
    public String createTransfer(Socket sock) {
        String transferCode;
        
        do {
            transferCode = generateCode();
        } while (this.transferCodes.get(transferCode) != null);
        
        this.transferCodes.put(transferCode, sock);
        
        return transferCode;
    }
    
    public Socket getTransfer(String transferCode) {
        return this.transferCodes.get(transferCode);
    }
    
    public void stopTransfer(String transferCode) {
        this.transferCodes.remove(transferCode);
    }
    
    public static String generateCode() {
        SecureRandom rng = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        
        for (int i = 0; i < CODE_LENGTH; ++i)
            codeBuilder.append(CODE_SET[rng.nextInt(CODE_SET.length)]);
        
        return codeBuilder.toString();
    }
}
