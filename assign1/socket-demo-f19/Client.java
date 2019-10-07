/*
CS640 Fall 2019
*/
import java.io.*;
import java.net.*;
 
public class Client {
    public static void main(String[] args) throws IOException {
         
        if (args.length != 2) {
                System.err.println(
                    "Usage: java Client <host name> <port number>");
                System.exit(1);
        }

        String host = args[0];
        int portNumber = Integer.parseInt(args[1]);
        System.out.println("Connecting to " + host + " @ port number " + portNumber);

        /* 1. Create a socket that connects to the server (identified by the host name and port number) */
        Socket clientSoc = new Socket(host, portNumber);
        System.out.println("Connection Established\n");

        /* 2. Get handles to the input and output stream of the socket */
        DataOutputStream outStream = new DataOutputStream(clientSoc.getOutputStream());
        PrintWriter writer = new PrintWriter(outStream, true);

        DataInputStream inStream = new DataInputStream(clientSoc.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

        /* 3. Get a handle to the standart input stream to get the user's input (that needs to be sent over to the server) */
        BufferedReader stdIn = new BufferedReader (new InputStreamReader (System.in));

        System.out.println("Enter data to be sent to server: ");
        String stdInput;
        /* 4a. Block until the user enters data to the standard input stream */
        /* 4b. Write the users input the input stream of the socket (sends data to the server) */
        /* 4c. Read the output stream of the socket (reads data sent by the server) */
        while ((stdInput = stdIn.readLine()) !=null){
                writer.println(stdInput);
                System.out.println("Text received --> " + reader.readLine());
        }
        
        /* 5. Close the socket */
        clientSoc.close();
	}
}
