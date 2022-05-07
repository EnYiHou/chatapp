package server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Scanner;

public class ServerFrontend {
    public static void main(String[] args)
        throws IOException, InterruptedException,
            HashInvalidLengthException,
            NoSuchAlgorithmException, SQLException {
        Server server = new Server();
        
        System.out.println("listening...");
        server.listen();
        
        Scanner sc = new Scanner(System.in);
        
        System.out.print("Enter to stop: ");
        sc.nextLine();
        
        if (server.isListening())
            server.close();
        
        System.out.println("Done");
    }
}
