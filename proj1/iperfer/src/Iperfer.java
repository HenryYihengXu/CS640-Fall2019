import java.net.*;
import java.io.*;

public class Iperfer {

    public static void main(String[] args) throws IOException {
        if (args.length != 7 && args.length != 3) {
           System.err.println("Error: invalid arguments");
           System.exit(1);
        }
        String mode = args[0];
        if (!mode.equals("-c") && !mode.equals("-s")) {
            System.err.println("Error: invalid arguments");
            System.exit(1);
        }
        if (mode.equals("-c")) {
            if (args.length != 7) {
                System.err.println("Error: invalid arguments");
                System.exit(1);
            }
            if (!args[1].equals("-h") || !args[3].equals("-p") || !args[5].equals("-t")) {
                System.err.println("Error: invalid arguments");
                System.exit(1);
            }
            String host = args[2];
            String[] addresses = host.split("[.]");
            if (addresses.length != 4) {
                System.err.println("Error: invalid host name");
                System.exit(1);
            }
            int section0 = 0;
            int section1 = 0;
            int section2 = 0;
            int section3 = 0;
            try {
                section0 = Integer.parseInt(addresses[0]);
                section1 = Integer.parseInt(addresses[1]);
                section2 = Integer.parseInt(addresses[2]);
                section3 = Integer.parseInt(addresses[3]);
            } catch (NumberFormatException e) {
                System.err.println("Error: invalid host name");
                System.exit(1);
            }
            if (section0 < 0 || section0 > 255
                    || section1 < 0 || section1 > 255
                    || section2 < 0 || section2 > 255
                    || section3 < 0 || section3 > 255) {
                System.err.println("Error: each section of the host name must be in the range 0 to 255");
                System.exit(1);
            }
            int portNumber = 0;
            try {
                portNumber = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                System.err.println("Error: port number must be in the range 1024 to 65535");
                System.exit(1);
            }
            if (portNumber < 1024 || portNumber > 65535) {
                System.err.println("Error: port number must be in the range 1024 to 65535");
                System.exit(1);
            }
            int time = 0;
            try {
                time = Integer.parseInt(args[6]);
            } catch (NumberFormatException e) {
                System.err.println("Error: invalid time");
                System.exit(1);
            }
            if (time < 0) {
                System.err.println("Error: time must be greater than 0");
                System.exit(1);
            }

            Client client = new Client(host, portNumber, time);


        } else {
            if (args.length != 3) {
                System.err.println("Error: invalid arguments");
                System.exit(1);
            }
            if (!args[1].equals("-p")) {
                System.err.println("Error: invalid arguments");
                System.exit(1);
            }
            int portNumber = 0;
            try {
                portNumber = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                System.err.println("Error: port number must be in the range 1024 to 65535");
                System.exit(1);
            }
            if (portNumber < 1024 || portNumber > 65535) {
                System.err.println("Error: port number must be in the range 1024 to 65535");
                System.exit(1);
            }

            Server server = new Server(portNumber);
        }
    }
}
