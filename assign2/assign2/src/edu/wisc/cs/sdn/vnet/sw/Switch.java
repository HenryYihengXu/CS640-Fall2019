package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
// we added these lines {
import net.floodlightcontroller.packet.MACAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static java.lang.System.currentTimeMillis;

import java.util.Map.Entry;
//}

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device {
	// we added these lines {
	private Map<MACAddress, IfaceTimePair> swithTable;
	MyTimer myTimer;
	// }

	/**
	 * Creates a router for a specific host.
	 * 
	 * @param host hostname for the router
	 */

	class MyTimer implements Runnable {
		private Map<MACAddress, IfaceTimePair> swithTable;

		public MyTimer(Map<MACAddress, IfaceTimePair> swithTable) {
			// store parameter for later user
			this.swithTable = swithTable;
		}

		public void run() {

			while (true) {
				synchronized (this.swithTable) {
					Iterator<Entry<MACAddress, IfaceTimePair>> it = swithTable.entrySet().iterator();
					while (it.hasNext()) {
						Entry<MACAddress, IfaceTimePair> entry = it.next();
						if (currentTimeMillis() - entry.getValue().getTime() > 15000) {
							it.remove();
						}
					}
				}
			}
		}
	}

	class IfaceTimePair {
		private Iface iface;
		private long time;

		IfaceTimePair(Iface iface, long time) {
			this.iface = iface;
			this.time = time;
		}

		public void setTime(long time) {
			this.time = time;
		}

		public void setIface(Iface iface) {
			this.iface = iface;
		}

		public long getTime() {
			return this.time;
		}

		public Iface getIface() {
			return this.iface;
		}

		public String toString() {
			return "iface: " + iface + ", time: " + time;
		}
	}

	public Switch(String host, DumpFile logfile) {
		super(host, logfile);
		swithTable = new HashMap<>();// *
		myTimer = new MyTimer(swithTable);
		new Thread(myTimer).start();
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

		synchronized (this.swithTable) {

			MACAddress destMAC = etherPacket.getDestinationMAC();
			MACAddress srcMAC = etherPacket.getSourceMAC();
			// check if we already know how to get to the destination
			IfaceTimePair destIfaceTimePair = this.swithTable.get(destMAC);

			// Learning. Whether the interface for srcMAC exists or not, we add the value/
			// modify the existing value
			this.swithTable.put(srcMAC, new IfaceTimePair(inIface, currentTimeMillis()));

			// if the destination doesn't exist in the switch table
			if (destIfaceTimePair == null) {
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
			// if the destination exists in the table, we send the packet to
			// the destination
			else {
				sendPacket(etherPacket, destIfaceTimePair.getIface());
			}
		}
		/********************************************************************/
	}
}

