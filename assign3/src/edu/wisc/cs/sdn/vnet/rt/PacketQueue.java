package edu.wisc.cs.sdn.vnet.rt;

import java.util.LinkedList;
import java.util.Queue;
import net.floodlightcontroller.packet.*;
import edu.wisc.cs.sdn.vnet.Iface;

public class PacketQueue implements Runnable {

    private Queue<Ethernet> packetQueue;
    private Queue<Iface> inIfaceQueue;
    private int counter;
    private Thread sendRequestThread;
    private Router myRouter;
    private Iface firstInIface;
    private int ipAddress;
    private boolean isReplied;
    private Iface outIface;

    public PacketQueue(Router router, Iface firstInIface, Iface outIface, int ipAddress) {
        this.packetQueue = new LinkedList<>();
        this.inIfaceQueue = new LinkedList<>();
        this.myRouter = router;
        this.firstInIface = firstInIface;
        this.ipAddress = ipAddress;
        this.isReplied = false;
        this.outIface = outIface;
        this.sendRequestThread = new Thread(this);
        this.sendRequestThread.start();
    }

    public void enqueue(Ethernet etherPacket, Iface inIface) {
        synchronized (packetQueue) {
            packetQueue.add(etherPacket);
            inIfaceQueue.add(inIface);
        }
    }

    public void dequeue(byte[] MAC) {
        synchronized (packetQueue) {
            System.out.println("seting isReplied to true");
            System.out.println("MAC: " + MAC);
            isReplied = true;
            for (int i = 0; i < packetQueue.size(); i++) {
                Ethernet etherPacket = packetQueue.poll();
                etherPacket.setDestinationMACAddress(MAC);
                myRouter.sendPacket(etherPacket, outIface);
            }
        }
    }

    public void setReplied() {
        this.isReplied = true;
    }

    public void run() {
        counter = 0;

        while (counter < 3 && !isReplied) {
            System.out.println("inside thread sending the " + counter + " request isRep : " + isReplied);
            generateArpRequest();
            counter++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }

        }
        synchronized (packetQueue) {
            if (!isReplied) {// need for loop
                Ethernet etherPacket;
                Iface inIface;
                for (int i = 0; i < packetQueue.size(); i++) {
                    etherPacket = packetQueue.poll();
                    inIface = inIfaceQueue.poll();
                    myRouter.generateICMP(etherPacket, inIface, (byte) 3, (byte) 1);
                }
            }
            myRouter.getQueueMap().remove(ipAddress);
        }

    }

    private void generateArpRequest() {
        // System.out.println();
        Ethernet replyEther = new Ethernet();
        ARP replyARP = new ARP();

        replyEther.setEtherType(Ethernet.TYPE_ARP);
        replyEther.setSourceMACAddress(firstInIface.getMacAddress().toBytes());
        replyEther.setDestinationMACAddress("FF:FF:FF:FF:FF:FF");

        replyARP.setHardwareType(ARP.HW_TYPE_ETHERNET);
        replyARP.setProtocolType(ARP.PROTO_TYPE_IP);
        replyARP.setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH);
        replyARP.setProtocolAddressLength((byte) 4);
        replyARP.setOpCode(ARP.OP_REQUEST);
        replyARP.setSenderHardwareAddress(firstInIface.getMacAddress().toBytes());
        replyARP.setSenderProtocolAddress(firstInIface.getIpAddress());
        // replyARP.setTargetHardwareAddress(arpPacket.getSenderHardwareAddress());
        replyARP.setTargetHardwareAddress(new byte[6]);
        replyARP.setTargetProtocolAddress(ipAddress);

        replyEther.setPayload(replyARP);

        // System.out.println("Sending: inIface IP address: " +
        // firstInIface.getIpAddress());
        // System.out.println("Sending: inIface Mac: " + firstInIface.getMacAddress());

        // System.out.println("Sending: ipAddress: " + ipAddress);

        myRouter.sendPacket(replyEther, outIface);
        System.out.println("ARP request generated\n");
    }
}