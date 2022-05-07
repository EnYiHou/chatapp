package protocol;

import java.util.Objects;

public class Message {
    private final String message;
    private final String author;
    private final long timestamp;
    
    public Message(String message, String author, long timestamp) {
        this.message = message;
        this.author = author;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getAuthor() {
        return author;
    }
    
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.message);
        hash = 29 * hash + Objects.hashCode(this.author);
        hash = 29 * hash + (int) (this.timestamp ^ (this.timestamp >>> 32));
        return hash;
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
        final Message other = (Message) obj;
        
        
        return
            this.timestamp == other.timestamp &&
            this.author.equals(other.author) &&
            this.message.equals(other.message);
    }
}
