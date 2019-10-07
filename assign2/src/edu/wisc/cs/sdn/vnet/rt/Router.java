package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.*;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device {
	/** Routing table for the router */
	private RouteTable routeTable;

	/** ARP cache for the router */
	private ArpCache arpCache;

	/**
	 * Creates a router for a specific host.
	 *
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile) {
		super(host, logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}

	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable() {
		return this.routeTable;
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

		// where should we use inIface????????

		// check if it contains an IPv4 packet:
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4) {
			System.out.println(
					"packet is not IPv4 packet: " + Ethernet.TYPE_IPv4 + ", it is: " + etherPacket.getEtherType());
			return;
		}

		IPv4 header = (IPv4) etherPacket.getPayload();

		// verify the checksum:
		// keep the current checksum
		short expectedChecksum = header.getChecksum();
		// set the checksum to be 0
		header.resetChecksum();
		// we compute the checksum bases on other values in header
		header.serialize();
		short actualChecksum = header.getChecksum();
		// if the newly computed checksum is not the same as the checksum stored, it
		// might be corrupted somewhere
		System.out.println("before actualChecksum != expectedChecksum");
		if (actualChecksum != expectedChecksum) {
			return;
		}

		// verify the TTL
		byte ttl = header.getTtl();
		// decremtn ttl because ttl should posses the counter how many rounters can use?
		ttl--;
		System.out.println("ttl <= 0");
		if (ttl <= 0) {
			return;

		}
		header.setTtl(ttl);

		// recompute checksum
		header.serialize();
		short newChecksum = header.getChecksum();
		header.setChecksum(newChecksum);
		// determine whether the packet is destined for one of the router’s interfaces
		int destinationAddress = header.getDestinationAddress();
		System.out.println("determine whether the packet is destined for one of the router’s interfaces");
		for (Iface iface : this.interfaces.values()) {
			if (destinationAddress == iface.getIpAddress()) {
				return;
			}
		}

		// forwarding the packets
		RouteEntry re = this.routeTable.lookup(destinationAddress);
		System.out.println("RouteEntry re is null");
		if (re == null) {
			return;
		}
		// Drop packet if the Iface where it should be sent out is same as the Iface
		// where it comes from
		Iface outIface = re.getInterface();
		System.out.println(" Drop packet if the Iface where it should be sent out is same as the Iface ");
		if (outIface.equals(inIface)) {
			return;
		}

		// Look up the MAC of next hop.
		MACAddress nextMAC = null;
		int gateWayAddress = re.getGatewayAddress();
		// if gateway addr is 0, we should use dest ip addr
		// because this router directly connected to the switch network where the host
		// is in.
		if (gateWayAddress == 0) {
			if (destinationAddress != re.getDestinationAddress()) {
				System.out.println("Error: destinationAddress != re.getDestinationAddress()");
			}
			nextMAC = this.arpCache.lookup(destinationAddress).getMac();
		}
		// otherwise use gateway addr because it only knows which router to send next,
		// which gateWayAddress shows
		else {
			nextMAC = this.arpCache.lookup(gateWayAddress).getMac();
		}

		// changing the source MAC and destination MAC
		etherPacket.setDestinationMACAddress(nextMAC.toBytes());
		etherPacket.setSourceMACAddress(outIface.getMacAddress().toBytes());

		// send the packet
		sendPacket(etherPacket, outIface);

		/********************************************************************/
	}
}
