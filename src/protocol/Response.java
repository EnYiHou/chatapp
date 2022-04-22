package protocol;

public class Response {
    private final EResponseType type;
    private final byte body[];
    
    public Response(EResponseType type, byte body[]) {
        this.type = type;
        this.body = body;
    }

    public EResponseType getType() {
        return this.type;
    }

    public byte[] getBody() {
        return this.body;
    }
}
