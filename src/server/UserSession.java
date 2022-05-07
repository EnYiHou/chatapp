package server;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import protocol.Cookie;
import protocol.IntegerSerializer;
import protocol.ProtocolFormatException;
import protocol.Response;
import protocol.ResponseSerializer;

public class UserSession {
    private final String username;
    private final Socket notificationSocket;
    private final Cookie cookie;
    private int conversation;
    private boolean dead;
    
    public UserSession(String username, Socket notificationSocket) {
        this.username = username;
        this.cookie = new Cookie();
        this.notificationSocket = notificationSocket;
        this.conversation = -1;
        this.dead = false;
    }

    public String getUsername() {
        return username;
    }

    public Cookie getCookie() {
        return cookie;
    }
    
    public boolean isDead() {
        return this.dead;
    }
    
    public void sendNotification(Response resp) throws ProtocolFormatException {
        byte[] rawResponse = new ResponseSerializer().serialize(resp);
        
        try {
            this.notificationSocket.getOutputStream().write(
                new IntegerSerializer().serialize(rawResponse.length)
            );

            this.notificationSocket.getOutputStream().write(rawResponse);
        } catch (IOException ex) {
            this.dead = true;
        }
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
        
        return
            this.username.equals(other.username) &&
            this.cookie.equals(other.cookie) &&
            this.notificationSocket.equals(other.notificationSocket);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.username);
        hash = 37 * hash + Objects.hashCode(this.notificationSocket);
        hash = 37 * hash + Objects.hashCode(this.cookie);
        return hash;
    }

    public int getConversation() {
        return conversation;
    }

    public void setConversation(int conversation) {
        this.conversation = conversation;
    }

    @Override
    public String toString() {
        return "UserSession{" + "username=" + username + ", notificationSocket=" + notificationSocket + ", cookie=" + cookie + ", conversation=" + conversation + '}';
    }
}
