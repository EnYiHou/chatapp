package client;

import java.io.IOException;
import java.util.Scanner;
import protocol.ProtocolFormatException;


public class ClientFrontend {
    private static Scanner sc;
    
    private static Client connectClient() {
        String host;
        int port;
        Client client;
        
        System.out.print("Enter server hostname: ");
        host = sc.nextLine();
        
        do {
            String input;
            
            System.out.print("Enter server port (nothing for default): ");
            input = sc.nextLine();
            
            try {
                port = Integer.parseInt(input);
                
                if (port <= 0 || port > 65535)
                    throw new NumberFormatException();
                
                break;
            } catch (NumberFormatException e) {
                System.err.println("error: invalid port number");
            }
        } while (true);
        
        client = new Client(host, port);
        
        do {
            try {
                String username;
                boolean signingUp;
                
                do {
                    String input;
                    
                    System.out.print("Do you want to sign up? (y/n): ");
                    
                    input = sc.nextLine();
                    if (
                        !"y".equalsIgnoreCase(input) &&
                        !"n".equalsIgnoreCase(input)
                    ) {
                        System.err.println("You must enter either y or n");
                        continue;
                    }
                    
                    signingUp = "y".equalsIgnoreCase(input);
                    break;
                } while (true);
                
                System.out.print("Enter username: ");
                username = sc.nextLine();
                
                System.out.print("Enter password: ");
                
                client.authenticate(
                    username,
                    sc.nextLine(),
                    signingUp
                );
                
                break;
            } catch (ServerErrorException ex) {
                System.err.println(
                    "error: server responded with: " + ex.getMessage()
                );
            } catch (IOException ex) {
                System.err.println("error: connection failed");
                return null;
            } catch (ProtocolFormatException ex) {
                System.err.println("error: " + ex.getMessage());
                return null;
            }
        } while (true);
        
        return client;
    }
    
    
    public static void main(String[] args) {
        sc = new Scanner(System.in);
        Client client = connectClient();

        
    }
}