package protocol;

import java.util.regex.Pattern;

public class User {
    private final String username;

    public User(String username) throws ProtocolFormatException {
        Pattern pattern = Pattern.compile("^[A-Za-z0-9_ ]{3,}$");
        
        if (!pattern.matcher(username).matches())
            throw new ProtocolFormatException("Invalid username");
        
        this.username = username;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
            
        if (obj == null)
            return false;
        
        if (this.getClass() != obj.getClass())
            return false;
        
        final User other = (User)obj;
        
        return this.username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return this.username.hashCode();
    }
}
