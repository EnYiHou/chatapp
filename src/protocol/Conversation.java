package protocol;

import java.util.List;

public class Conversation {
    private final String code;
    private final String name;
    private final List<String> messages;
    
    public Conversation(String code, String name, List<String> messages) {
        this.code = code;
        this.name = name;
        this.messages = messages;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public List<String> getMessages() {
        return messages;
    }
}
