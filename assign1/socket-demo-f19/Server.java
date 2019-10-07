/*
CS640 Fall 2019
*/
import java.net.*;
import java.io.*;

public class Server {
    public static void main(String[] args) throws IOException {
        
        if (args.length != 1) {
            System.err.println("Usage: java Server <port number>");
            System.exit(1);
        }

        int serverPort = Integer.parseInt(args[0]);

        /* 1. Create a ServerSocket that listens on the specified port */
        ServerSocket serverSoc = new ServerSocket(serverPort);	

        System.out.println("Waiting for client connections");
        /* 2. Block until a client requests a connection to this application */
        Socket clientSoc = serverSoc.accept();

        /* 3. Get handles to the output and input stream of the socket */
        DataOutputStream outStream = new DataOutputStream(clientSoc.getOutputStream());
        PrintWriter writer = new PrintWriter(outStream, true);

        DataInputStream inStream = new DataInputStream(clientSoc.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));

        System.out.println("Connection established\n");
        String text;
        /* 4. Block until you read a line from the client (Incoming data can read from the input stream) */

        /* 5. Echo back the line read from the client (Write the incoming data to the output stream) */
        /* 5a. Read data bytes sent from client using the input stream */ 
        /* 5b. Print the data received to the standard output */
        /* 5c. Send back the data received using the output stream */
        while ((text = reader.readLine()) != null){
                System.out.println("Text received ==> " + text);
                writer.println(text);
        }

        /* 6. Close sockets */
        clientSoc.close();
        serverSoc.close();
	}
}
