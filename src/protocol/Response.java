package protocol;

public class Response {
    private final EResponseType type;
    private final String message;
    
    public Response(EResponseType type, String message) {
        this.type = type;
        this.message = message;
    }

    public EResponseType getType() {
        return this.type;
    }

    public String getMessage() {
        return this.message;
    }
}
