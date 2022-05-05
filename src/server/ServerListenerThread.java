package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerListenerThread extends Thread {
    private final ExecutorService handlers;
    private final int port;
    private final AuthenticationManager authManager;
    private final MessageManager messageManager;
    private final SecretManager secretManager;
    
    public ServerListenerThread(
        int port,
        SecretManager secretManager,
        AuthenticationManager authManager,
        MessageManager messageManager
    ) throws IOException {
        this.handlers = Executors.newCachedThreadPool();
        this.port = port;
        this.authManager = authManager;
        this.messageManager = messageManager;
        this.secretManager = secretManager;
    }

    @Override
    public void run() {
        try (ServerSocket sock = new ServerSocket(port)) {
            sock.setReuseAddress(true);
            sock.setSoTimeout(500);
            
            while (true) {
                try {
                    Socket conn = sock.accept();
                    this.handlers.execute(new ConnectionHandler(
                        conn,
                        this.secretManager,
                        this.authManager,
                        this.messageManager
                    ));
                } catch (SocketTimeoutException e) {
                    if (this.isInterrupted())
                        break;
                } catch (IOException e) {
                    System.err.println(e);
                }
            }

            sock.close();
        } catch (IOException ex) {
            System.err.println(ex);
        }
        
        this.handlers.shutdown();
    }
}
