import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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
        PrintWriter writer = new PrintWriter(outStream, true);

        DataInputStream inStream = new DataInputStream(clientSoc.getInputStream());
        BufferedReader reader = new BufferedReader((new InputStreamReader(inStream)));

        char c = '\0';
        int length = 1000;
        char[] zeroArray = new char[length];
        Arrays.fill(zeroArray, c);
        String sendingData = new String(zeroArray);

        int counter = 0;
        for (long stop = System.nanoTime() + TimeUnit.SECONDS.toNanos(time); stop > System.nanoTime();) {
            writer.print(sendingData);
            counter++;
            System.out.println("1");
        }

        clientSoc.close();
        System.out.println("sent " + counter + " KB rate=" + counter / 1000 / time + " Mbps");

        // char c = '0';
        // int length = 1000;
        // byte[] zeroArray = new byte[length];
        // Arrays.fill(zeroArray, 0x0);
        // // String sendingData = new String(chars);

        // for(long
        // stop=System.nanoTime()+TimeUnit.SECONDS.toNanos(time);stop>System.nanoTime();)
        // {
        // writer.print
        // }

        // while ((stdInput = stdIn.readLine()) !=null){
        // writer.println(stdInput);
        // System.out.println("Text received --> " + reader.readLine());
        // }
    }

    // // send the chunk of data
    // Timer timer = new Timer();
    // TimerTask task = new Task();

    // timer.schedule(task, 0, time * 1000);
}

// class Task extends TimerTask {

// public void run(String sendingData) {
// System.out.println("sending data!");
// }
// }