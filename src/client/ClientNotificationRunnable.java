package client;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import protocol.Cookie;
import protocol.CookieSerializer;
import protocol.CredentialsBody;
import protocol.CredentialsBodySerializer;
import protocol.ERequestType;
import protocol.IntegerSerializer;
import protocol.Message;
import protocol.MessageSerializer;
import protocol.ProtocolFormatException;
import protocol.Request;
import protocol.RequestSerializer;
import protocol.Response;
import protocol.ResponseSerializer;
import protocol.StringSerializer;

public class ClientNotificationRunnable implements Runnable {
    private final Socket sock;
    private final Cookie cookie;
    private final LimitedPriorityBlockingQueue<Message> messages;
    
    public ClientNotificationRunnable(
        String host,
        int port,
        String username,
        String password,
        boolean signingUp,
        LimitedPriorityBlockingQueue<Message> messages
    ) throws IOException, ProtocolFormatException, ServerErrorException {
        this.sock = new Socket(host, port);

        this.messages = messages;
        
        byte[] credRequest = new RequestSerializer().serialize(
            new Request(
                signingUp ? ERequestType.SIGNUP : ERequestType.LOGIN,
                Cookie.emptyCookie(),
                new CredentialsBodySerializer().serialize(
                    new CredentialsBody(username, password)
                )
            )
        );
        
        this.sock.getOutputStream().write(
            new IntegerSerializer().serialize(credRequest.length)
        );
        
        this.sock.getOutputStream().write(credRequest);
        
        Response resp = new ResponseSerializer().deserialize(
            this.sock.getInputStream().readNBytes(
                new IntegerSerializer().deserialize(
                    this.sock.getInputStream().readNBytes(
                        IntegerSerializer.size()
                    )
                ).getValue()
            )
        ).getValue();

        this.sock.setSoTimeout(500);
        
        switch (resp.getType()) {
            case COOKIE:
                this.cookie = new CookieSerializer().deserialize(
                    resp.getBody()
                ).getValue();
                break;
            case ERROR:
                throw new ServerErrorException(
                    new StringSerializer().deserialize(
                        resp.getBody()
                    ).getValue()
                );
            default:
                throw new ProtocolFormatException(
                    "Bad response type for request"
                );
        }
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                if (Thread.interrupted())
                    return;
                
                Response resp = new ResponseSerializer().deserialize(
                    this.sock.getInputStream().readNBytes(
                        new IntegerSerializer().deserialize(
                            this.sock.getInputStream().readNBytes(
                                IntegerSerializer.size()
                            )
                        ).getValue()
                    )
                ).getValue();

                switch (resp.getType()) {
                    case COOKIE:
                        throw new ProtocolFormatException(
                            "Attempt to reset cookie"
                        );
                    case MESSAGE:
                        this.messages.add(new MessageSerializer().deserialize(
                            resp.getBody()
                        ).getValue());

                        break;
                    case ERROR:
                        throw new ServerErrorException(
                            new StringSerializer().deserialize(
                                resp.getBody()
                            ).getValue()
                        );
                    default:
                        throw new ProtocolFormatException(
                            "Bad response type for request"
                        );
                }
            } catch (SocketTimeoutException ex) {
            } catch (
                ServerErrorException | IOException | ProtocolFormatException ex
            ) {
                ex.printStackTrace();
                return;
            }
        }
    }
    
    public Cookie getCookie() {
        return this.cookie;
    }
}
