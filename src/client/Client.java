package client;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import protocol.Conversation;
import protocol.ConversationSerializer;
import protocol.ERequestType;
import protocol.EResponseType;
import protocol.IntegerSerializer;
import protocol.ProtocolFormatException;
import protocol.Request;
import protocol.RequestSerializer;
import protocol.Response;
import protocol.ResponseSerializer;
import protocol.StringSerializer;

public class Client {
    private final String host;
    private final int port;
    ClientNotificationRunnable runnable;
    Thread notificationThread;
        
    Client(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    private Response request(Request req, EResponseType expectedResponseType)
        throws ProtocolFormatException, IOException, ServerErrorException {
        Socket sock = new Socket(host, port);
        
        byte[] serializedRequest = new RequestSerializer().serialize(req);
        
        sock.getOutputStream().write(
            new IntegerSerializer().serialize(serializedRequest.length)
        );
        
        sock.getOutputStream().write(serializedRequest);
        
        
        
        Response resp = new ResponseSerializer().deserialize(
            sock.getInputStream().readNBytes(
                new IntegerSerializer().deserialize(
                    sock.getInputStream().readNBytes(
                        IntegerSerializer.size()
                    )
                ).getValue()
            )
        ).getValue();
                
        if (resp.getType() != expectedResponseType) {
            if (resp.getType() != EResponseType.ERROR)
                throw new ProtocolFormatException(
                    "Got invalid response type for LOGOUT request"
                );
            
            throw new ServerErrorException(
                new StringSerializer().deserialize(
                    resp.getBody()
                ).getValue()
            );
        }
        
        return resp;
    }
    
    public void authenticate(
        String username,
        String password,
        boolean signingUp
    ) throws ServerErrorException, IOException, ProtocolFormatException {
        this.runnable = new ClientNotificationRunnable(
            this.host,
            this.port,
            username,
            password,
            signingUp
        );
        
        this.notificationThread = new Thread(this.runnable);
    }
    
    public void logout()
        throws ProtocolFormatException, IOException, ServerErrorException {
        
        this.request(
            new Request(
                ERequestType.LOGOUT,
                this.runnable.getCookie(),
                new byte[0]
            ),
            EResponseType.EMPTY
        );
        
        this.notificationThread.interrupt();
        try {
            this.notificationThread.join();
        } catch (InterruptedException ex) {
        }
    }
    
    public Conversation createConversation(String name)
        throws ProtocolFormatException, IOException, ServerErrorException {
        Response resp = this.request(
            new Request(
                ERequestType.CREATE_CONVO,
                this.runnable.getCookie(),
                new ConversationSerializer().serialize(
                    new Conversation("", name, List.of())
                )
            ),
            EResponseType.CONVERSATION
        );
        
        return new ConversationSerializer().deserialize(
            resp.getBody()
        ).getValue();
    }
}
