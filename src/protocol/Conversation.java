package protocol;

public class Conversation {
    private final String code;
    private final String name;

    public Conversation(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
