package edu.wisc.cs.sdn.vnet.rt;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Queue;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
// import all headers
import net.floodlightcontroller.packet.*;

import java.util.*;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device {
	/** Routing table for the router */
	private RouteTable routeTable;

	/** ARP cache for the router */
	private ArpCache arpCache;

	private Map<Integer, PacketQueue> queueMap;

	/**
	 * Creates a router for a specific host.
	 * 
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile) {
		super(host, logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
		this.queueMap = new HashMap<>();
	}

	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable() {
		return this.routeTable;
	}

	public Map<Integer, PacketQueue> getQueueMap() {
		return this.queueMap;
	}

	/**
	 * Load a new routing table from a file.
	 * 
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile) {
		if (!routeTable.load(routeTableFile, this)) {
			System.err.println("Error setting up routing table from file " + routeTableFile);
			System.exit(1);
		}

		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}

	/**
	 * Load a new ARP cache from a file.
	 * 
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile) {
		if (!arpCache.load(arpCacheFile)) {
			System.err.println("Error setting up ARP cache from file " + arpCacheFile);
			System.exit(1);
		}

		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * 
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface     the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface) {
		System.out.println("*** -> Received packet: " + etherPacket.toString().replace("\n", "\n\t"));

		/********************************************************************/
		/* TODO: Handle packets */

		switch (etherPacket.getEtherType()) {
		case Ethernet.TYPE_IPv4:
			this.handleIpPacket(etherPacket, inIface);
			break;
		case Ethernet.TYPE_ARP:
			this.handleArpPacket(etherPacket, inIface);
			break;
		// Ignore all other packet types, for now
		}

		/********************************************************************/
	}

	public void generateICMP(Ethernet originalPacket, Iface inIface, byte icmpType, byte icmpCode) {
		Ethernet ether = new Ethernet();
		IPv4 ip = new IPv4();
		ICMP icmp = new ICMP();
		Data data = new Data();

		IPv4 originalIPPacket = (IPv4) originalPacket.getPayload();

		// is this the correct way to add padding?
		short originalIPHeaderLen = originalIPPacket.getHeaderLength();
		byte[] byteArr = new byte[12 + originalIPHeaderLen * 4];
		for (int i = 0; i < 4; i++) {
			byteArr[i] = 0;
		}
		// is this the correct way to append first 8 bytes following original IP Header?
		byte[] originalIPPacketSerialized = originalIPPacket.serialize();
		for (int i = 0; i < originalIPHeaderLen * 4 + 8; i++) {
			byteArr[i + 4] = originalIPPacketSerialized[i];
		}
		data.setData(byteArr);

		icmp.setIcmpType(icmpType);
		icmp.setIcmpCode(icmpCode);
		icmp.resetChecksum();
		icmp.serialize();

		ip.setTtl((byte) 64);
		ip.setProtocol(IPv4.PROTOCOL_ICMP);
		ip.setSourceAddress(inIface.getIpAddress());
		// System.out.println("sourceIP: " + inIface.getIpAddress());
		ip.setDestinationAddress(originalIPPacket.getSourceAddress());
		// System.out.println("destinationIP: " + originalIPPacket.getSourceAddress());
		// ip.resetChecksum();
		// ip.serialize();

		ether.setEtherType(Ethernet.TYPE_IPv4);
		// get source MAC from the interface where the original packet arrived
		MACAddress sourceMAC = inIface.getMacAddress();
		// System.out.println("source MAC: " + sourceMAC);
		ether.setSourceMACAddress(sourceMAC.toBytes());
		// get destination MAC from source IP
		// ether.setDestinationMACAddress(originalPacket.getSourceMACAddress());
		//
		int dstAddr = originalIPPacket.getSourceAddress();
		RouteEntry bestMatch = this.routeTable.lookup(dstAddr);
		int nextHop = bestMatch.getGatewayAddress();
		if (0 == nextHop) {
			nextHop = dstAddr;
		}

		// Set destination MAC address in Ethernet header
		ArpEntry arpEntry = this.arpCache.lookup(nextHop);
		if (null == arpEntry) {
			return;
		}
		ether.setDestinationMACAddress(arpEntry.getMac().toBytes());
		//
		// System.out.println("destination MAC: " + originalPacket.getSourceMAC());
		// System.out.println();

		icmp.setPayload(data);
		ip.setPayload(icmp);
		ether.setPayload(ip);
		// System.out.println(inIface.getName());

		sendPacket(ether, inIface);
	}

	private void generateEchoReply(Ethernet originalPacket, Iface inIface) {
		Ethernet ether = new Ethernet();
		IPv4 ip = new IPv4();
		ICMP icmp = new ICMP();
		Data data = (Data) (originalPacket.getPayload().getPayload().getPayload());

		IPv4 originalIPPacket = (IPv4) originalPacket.getPayload();

		icmp.setIcmpType((byte) 0);
		icmp.setIcmpCode((byte) 0);
		icmp.resetChecksum();
		icmp.serialize();

		ip.setTtl((byte) 64);
		ip.setProtocol(IPv4.PROTOCOL_ICMP);
		ip.setSourceAddress(originalIPPacket.getDestinationAddress());
		// System.out.println("sourceIP: " + originalIPPacket.getDestinationAddress());
		ip.setDestinationAddress(originalIPPacket.getSourceAddress());
		// System.out.println("destinationIP: " + originalIPPacket.getSourceAddress());
		// ip.resetChecksum();
		// ip.serialize();

		ether.setEtherType(Ethernet.TYPE_IPv4);
		// get source MAC from the interface where the original packet arrived
		MACAddress sourceMAC = inIface.getMacAddress();
		// System.out.println("source MAC: " + sourceMAC);
		ether.setSourceMACAddress(sourceMAC.toBytes());
		// get destination MAC from source IP
		// ether.setDestinationMACAddress(originalPacket.getSourceMACAddress());
		//
		int dstAddr = originalIPPacket.getSourceAddress();
		RouteEntry bestMatch = this.routeTable.lookup(dstAddr);
		int nextHop = bestMatch.getGatewayAddress();
		if (0 == nextHop) {
			nextHop = dstAddr;
		}

		// Set destination MAC address in Ethernet header
		ArpEntry arpEntry = this.arpCache.lookup(nextHop);
		if (null == arpEntry) {
			return;
		}
		ether.setDestinationMACAddress(arpEntry.getMac().toBytes());
		//
		// System.out.println("destination MAC: " + originalPacket.getSourceMAC());
		// System.out.println();

		icmp.setPayload(data);
		ip.setPayload(icmp);
		ether.setPayload(ip);
		// System.out.println(inIface.getName());

		sendPacket(ether, inIface);
	}

	private void generateArpReply(Ethernet etherPacket, Iface inIface) {
		ARP arpPacket = (ARP) etherPacket.getPayload();
		int targetIp = ByteBuffer.wrap(arpPacket.getTargetProtocolAddress()).getInt();
		if (targetIp != inIface.getIpAddress()) {
			System.out.println("arpPacket.targetIp != inIface.getIpAddress()");
			return;
		}

		Ethernet replyEther = new Ethernet();
		ARP replyARP = new ARP();

		replyEther.setEtherType(Ethernet.TYPE_ARP);
		replyEther.setSourceMACAddress(inIface.getMacAddress().toBytes());
		replyEther.setDestinationMACAddress(etherPacket.getSourceMACAddress());

		replyARP.setHardwareType(ARP.HW_TYPE_ETHERNET);
		replyARP.setProtocolType(ARP.PROTO_TYPE_IP);
		replyARP.setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH);
		replyARP.setProtocolAddressLength((byte) 4);
		replyARP.setOpCode(ARP.OP_REPLY);
		replyARP.setSenderHardwareAddress(inIface.getMacAddress().toBytes());
		replyARP.setSenderProtocolAddress(inIface.getIpAddress());
		replyARP.setTargetHardwareAddress(arpPacket.getSenderHardwareAddress());
		replyARP.setTargetProtocolAddress(arpPacket.getSenderProtocolAddress());

		replyEther.setPayload(replyARP);

		sendPacket(replyEther, inIface);
		System.out.println("The end\n");
	}

	private void handleArpPacket(Ethernet etherPacket, Iface inIface) {

		if (etherPacket.getEtherType() != Ethernet.TYPE_ARP) {
			return;
		}
		System.out.println("receiving arp packet");
		ARP arpPacket = (ARP) etherPacket.getPayload();
		if (arpPacket.getOpCode() == ARP.OP_REQUEST) {
			generateArpReply(etherPacket, inIface);
			return;
		}
		if (arpPacket.getOpCode() == ARP.OP_REPLY) {
			System.out.println("receiving arp reply");
			int targetIp = ByteBuffer.wrap(arpPacket.getSenderProtocolAddress()).getInt();
			arpCache.insert(new MACAddress(arpPacket.getSenderHardwareAddress()), targetIp);
			if (queueMap.containsKey(targetIp)) {
				PacketQueue queue = queueMap.get(targetIp);
				queue.dequeue(arpPacket.getSenderHardwareAddress());
				// It should not matter
				queueMap.remove(targetIp);
				System.out.println("dequeued");
			}
			System.out.println("ARP reply received\n");
			return;
		}
	}

	private void handleIpPacket(Ethernet etherPacket, Iface inIface) {
		// Make sure it's an IP packet
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4) {
			return;
		}

		// Get IP header
		IPv4 ipPacket = (IPv4) etherPacket.getPayload();
		System.out.println("Handle IP packet");

		// Verify checksum
		short origCksum = ipPacket.getChecksum();
		ipPacket.resetChecksum();
		byte[] serialized = ipPacket.serialize();
		ipPacket.deserialize(serialized, 0, serialized.length);
		short calcCksum = ipPacket.getChecksum();
		if (origCksum != calcCksum) {
			return;
		}

		// Check TTL
		// System.out.println("TTL before decrementing " + ipPacket.getTtl());
		ipPacket.setTtl((byte) (ipPacket.getTtl() - 1));
		// drop the packet if TTL is 0
		if (0 == ipPacket.getTtl()) {
			generateICMP(etherPacket, inIface, (byte) 11, (byte) 0);
			// we might remove the return here.
			return;
		}

		// Reset checksum now that TTL is decremented
		ipPacket.resetChecksum();

		// Check if packet is destined for one of router's interfaces
		for (Iface iface : this.interfaces.values()) {
			if (ipPacket.getDestinationAddress() == iface.getIpAddress()) {
				if (ipPacket.getProtocol() == IPv4.PROTOCOL_TCP || ipPacket.getProtocol() == IPv4.PROTOCOL_UDP) {
					generateICMP(etherPacket, inIface, (byte) 3, (byte) 3);
				}
				if (ipPacket.getProtocol() == IPv4.PROTOCOL_ICMP && ((ICMP) ipPacket.getPayload()).getIcmpType() == 8) {
					// function to create the echo reply message
					generateEchoReply(etherPacket, inIface);
				}
				return;
			}
		}

		// Do route lookup and forward
		this.forwardIpPacket(etherPacket, inIface);
	}

	private void forwardIpPacket(Ethernet etherPacket, Iface inIface) {
		// Make sure it's an IP packet
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4) {
			return;
		}
		System.out.println("Forward IP packet");

		// Get IP header
		IPv4 ipPacket = (IPv4) etherPacket.getPayload();
		int dstAddr = ipPacket.getDestinationAddress();

		// Find matching route table entry
		RouteEntry bestMatch = this.routeTable.lookup(dstAddr);

		// If no entry matched, do nothing
		if (null == bestMatch) {
			generateICMP(etherPacket, inIface, (byte) 3, (byte) 0);
			return;
		}

		// Make sure we don't sent a packet back out the interface it came in
		Iface outIface = bestMatch.getInterface();
		if (outIface == inIface) {
			return;
		}

		// Set source MAC address in Ethernet header
		etherPacket.setSourceMACAddress(outIface.getMacAddress().toBytes());

		// If no gateway, then nextHop is IP destination
		int nextHop = bestMatch.getGatewayAddress();
		if (0 == nextHop) {
			nextHop = dstAddr;
		}

		// Set destination MAC address in Ethernet header
		ArpEntry arpEntry = this.arpCache.lookup(nextHop);
		if (null == arpEntry) {
			// generate a destination host unreachable ICMP
			// generateICMP(etherPacket, inIface, (byte) 3, (byte) 1);
			if (!queueMap.containsKey(nextHop)) {
				PacketQueue queue = new PacketQueue(this, inIface, outIface, nextHop);
				queueMap.put(nextHop, queue);
			}
			queueMap.get(nextHop).enqueue(etherPacket, inIface);
			return;
		}
		etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());

		this.sendPacket(etherPacket, outIface);
	}
}
