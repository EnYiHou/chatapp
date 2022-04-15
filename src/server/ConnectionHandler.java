package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import protocol.Deserialized;
import protocol.EResponseType;
import protocol.IntegerSerializer;
import protocol.ProtocolFormatException;
import protocol.Response;
import protocol.ResponseSerializer;

public class ConnectionHandler implements Runnable {
    private final Socket conn;
    
    ConnectionHandler(Socket conn) throws SocketException {
        this.conn = conn;
        this.conn.setSoTimeout(10000);
    }
    
    @Override
    public void run() {
        Response response = null;
        
        try {
            InputStream stream = this.conn.getInputStream();
            IntegerSerializer serializer = new IntegerSerializer();
            Deserialized<Integer> announcedSize = serializer.deserialize(
                stream.readNBytes(IntegerSerializer.size())
            );
            
            byte[] req = stream.readNBytes(announcedSize.getValue());
            
            System.out.println(Arrays.toString(req));
            response = new Response(EResponseType.SUCCESS, "success");
        } catch (SocketTimeoutException ex) {
            response = new Response(EResponseType.ERROR, "timeout");
        } catch (ProtocolFormatException ex) {
            response = new Response(EResponseType.ERROR, "invalid message");
        } catch (IOException ex) {
            response = new Response(EResponseType.ERROR, ex.getMessage());
        } finally {
            try {
                this.conn.getOutputStream().write(
                    new ResponseSerializer().serialize(response)
                );
                this.conn.shutdownInput();
                this.conn.shutdownOutput();
                this.conn.close();
            } catch (IOException | ProtocolFormatException ex) {
                System.err.println(ex);
            }
        }
    }
}
