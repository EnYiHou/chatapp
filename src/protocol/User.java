package protocol;

import java.util.Optional;

public class User {
    private String username;
    private Optional<String> password;

    public User(String username) {
        this.username = username;
    }
    
    public User(String username, Optional<String> password) {
        this.username = username;
        this.password = password;
    }
}
