import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

public class Client {

    private String host;
    private int portNumber;
    private int time;

    public Client(String host, int portNumber, int time) {
        this.host = host;
        this.portNumber = portNumber;
        this.time = time;
    }

    public void callClient() throws IOException {

        System.out.println("Connectiong to " + host + " @ port number " + portNumber);

        Socket clientSoc = new Socket(host, portNumber);
        System.out.println("Connection Established\n");

        DataOutputStream outStream = new DataOutputStream(clientSoc.getOutputStream());

        int length = 1000;
        byte[] zeroArray = new byte[length];
        Arrays.fill(zeroArray, (byte)0x0);

        int counter = 0;
        long stop = currentTimeMillis() + time * 1000;
        while (stop > currentTimeMillis()) {
            outStream.write(zeroArray);
            counter++;
        }

        clientSoc.close();
        double rate = (double)counter / (double)1000 / (double)time; 
        System.out.println("sent " + counter + " KB rate=" + rate + " Mbps");
    }

}


    

    
