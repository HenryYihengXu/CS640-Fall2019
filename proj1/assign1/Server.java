import java.io.*;
import java.net.*;

import static java.lang.System.currentTimeMillis;

public class Server {

    private int portNumber;
    private long dataReceived;
    private long elapsedTime;

    Server(int portNumber) {
        this.portNumber = portNumber;
        dataReceived = 0;
        elapsedTime = 0;
    }

    public void listen() throws IOException{

        ServerSocket serverSoc = new ServerSocket(portNumber);
        Socket clientSoc = serverSoc.accept();

        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
        char[] buffer = new char[10000];
        long startTime = 0;
        int data;

        if ((data = reader.read(buffer, 0, 1)) != -1) {
            startTime = currentTimeMillis();
            dataReceived += data;
            //System.out.println(data);
        }
        while ((data = reader.read(buffer, 0, 10000)) != -1) {
            dataReceived += data;
            //System.out.println(data);
        }
        elapsedTime = currentTimeMillis() - startTime;
        dataReceived = dataReceived / 1000;
        double rate = (double)dataReceived / (double)1000 / ((double)elapsedTime / (double)1000);

        System.out.println("received=" + dataReceived + " KB" + " rate=" + rate + " Mbps");
        clientSoc.close();
        serverSoc.close();
    }
}
