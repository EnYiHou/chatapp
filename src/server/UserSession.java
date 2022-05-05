package server;

import java.io.IOException;
import java.net.Socket;
import protocol.Cookie;
import protocol.ProtocolFormatException;
import protocol.Response;
import protocol.ResponseSerializer;

public class UserSession {
    private final String username;
    private final Socket notificationSocket;
    private final Cookie cookie;
    
    public UserSession(String username, Socket notificationSocket) {
        this.username = username;
        this.cookie = new Cookie();
        this.notificationSocket = notificationSocket;
    }

    public String getUsername() {
        return username;
    }

    public Cookie getCookie() {
        return cookie;
    }
    
    public void sendNotification(Response resp)
        throws IOException, ProtocolFormatException {
        this.notificationSocket.getOutputStream().write(
            new ResponseSerializer().serialize(resp)
        );
    }
        
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final UserSession other = (UserSession) obj;
        
        return this.username.equals(other.username) && this.cookie.equals(other.cookie);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.username.hashCode();
        hash = 37 * hash + this.cookie.hashCode();
        return hash;
    }
}
