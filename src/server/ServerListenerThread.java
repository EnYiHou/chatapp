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
    
    public ServerListenerThread(int port, Server server) throws IOException {
        this.handlers = Executors.newCachedThreadPool();
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket sock = new ServerSocket(port)) {
            sock.setReuseAddress(true);
            sock.setSoTimeout(500);
            
            while (true) {
                try {
                    Socket conn = sock.accept();
                    this.handlers.execute(new ConnectionHandler(conn));
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
