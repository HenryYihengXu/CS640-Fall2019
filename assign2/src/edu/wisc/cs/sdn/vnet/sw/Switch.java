package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
// we added these lines {
import net.floodlightcontroller.packet.MACAddress;
import java.util.HashMap;
import java.util.Map;
import static java.lang.System.currentTimeMillis;
//}

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device {
	// we added these lines {
	private Map<MACAddress, Iface> swithTable;
	private Map<MACAddress, Long> addressTimeOut;
	// }

	/**
	 * Creates a router for a specific host.
	 *
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile) {
		super(host, logfile);
		swithTable = new HashMap<>();// *
		addressTimeOut = new HashMap<>();
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
		/*
		 * TODO: Handle packets
		 */

		MACAddress destMAC = etherPacket.getDestinationMAC();
		MACAddress srcMAC = etherPacket.getSourceMAC();
		// check if we already know how to get to the destination
		Iface destIface = this.swithTable.get(destMAC);

		// Learning. Whether the interface for srcMAC exists or not, we add the value/
		// modify the existing value
		this.swithTable.put(srcMAC, inIface);
		this.addressTimeOut.put(srcMAC, currentTimeMillis());

		//System.out.println("Host Name: " + getHost());
		// if the destination doesn't exist in the switch table
		if (destIface == null) {
			Map<String, Iface> interfaces = this.getInterfaces();
			// we have to broadcast the packet to every direction except where it is from
			// this is to prevent circle
			for (Iface i : interfaces.values()) {
				// comparing interface
				//System.out.println("Interface name: " + i.toString());
				//System.out.println("Comparing to: " + inIface);
				if (!i.equals(inIface)) {
					//System.out.println("Sent to : " + i);
					boolean b = sendPacket(etherPacket, i);
					//System.out.println("Packet sent:" + b);
				}
				//System.out.println();
			}
		}
		// if the destination exists, but it was already timed out (15 seconds)
		else if (currentTimeMillis() - this.addressTimeOut.get(destMAC) > 15000) {
			// remove it from the table, because the path to the destMac might be dead
			this.swithTable.remove(destMAC);
			this.addressTimeOut.remove(destMAC);

			Map<String, Iface> interfaces = this.getInterfaces();
			// we have to broadcast the packet to every direction except where it is from
			// this is to prevent circle
			for (Iface i : interfaces.values()) {
				// comparing interface
				if (!i.equals(inIface)) {
					sendPacket(etherPacket, i);
				}
			}
		}
		// if the destination exists in the table & not timed out, we send the packet to
		// the destination
		else {
			sendPacket(etherPacket, destIface);
		}

		/********************************************************************/
	}
}
