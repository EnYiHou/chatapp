package client;

import java.io.IOException;
import java.net.Socket;

public class Client {
    Socket sock;
        
    Client(String host, int port) throws IOException {
        this.sock = new Socket(host, port);
    }
}
