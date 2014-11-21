package net.floodlightcontroller.currentstate;

import java.util.ArrayList;

import java.util.LinkedList;



import net.floodlightcontroller.core.FloodlightContext;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;

import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;


public interface ICurrentStateService extends IFloodlightService {
	
	
	/**
	 * Starts the process of discovering all the
	 * round trip times between switches and the controller
	 * @param cntx
	 */
	public void getAllControllerRtt(FloodlightContext cntx);
	
	
	/**
	 * Discovers the path and the server, whose path between itself
	 and the client offers the lowest latency.
	 * @param srcSw source switch
	 * @param srcPort source port
	 * @param dstSw destination switch
	 * @param dstPort destination port
	 * @param membersIPs list of the IPs of the servers
	 * @return the path and the server, whose path between itself
	 and the client offers the lowest latency.
	 */
	public Route getServerRoute(long srcSw, short srcPort, long dstSw,
			short dstPort, ArrayList<Integer> membersIPs);
	
	
	
	/**
	 * Discovers the path to a server with the lowest latency
	 * @param srcSw source switch
	 * @param srcPort source port
	 * @param dstSw destination switch
	 * @param dstPort destination port
	 * @return the path with the lowest latency
	 */
	public Route getRoute(long srcSw, short srcPort, long dstSw, short dstPort);
	
	
	/**
	 * Calculates the total latency of a route
	 * @param Route route
	 * @return the total latency of a route
	 */
	public long getRouteLatency(Route route);
	
	
	/**
	 * List of all devices discovered by the CurrentState module
	 * @return List of all devices discovered by the CurrentState module
	 */
	public LinkedList<IDevice> getDeviceList();
	
	
	
	/**
	 * Adds the Neigbour's link latency to a node
	 * @param l the link
	 * @param latency 
	 */
	public void addNeigbourLink(Link l, long latency);

	
	/**
	 * Updates the throughput value using the info
	 * on the Data of a received UDP packet
	 * @param data info about the throughput between a pair of nodes
	 */
	public void handleBandwithInfo(Data data);

	
	/**
	 * Discovers the path and the server, whose path between itself
	 and the client has the best throughput.
	 * @param srcSw source switch
	 * @param srcPort source port
	 * @param dstSw destination switch
	 * @param dstPort destination port
	 * @param membersIPs list of the IPs of the servers
	 * @return the path and the server, whose path between itself
	 and the client offers the lowest latency.
	 */
	public Route getServerRoutebyBandwith(long srcSw, short srcPort, long dstSw,
			short dstPort, ArrayList<Integer> members);

	
	/**
	 * Discovers the path to a server with the best throughput.
	 * @param srcSw source switch
	 * @param srcPort source port
	 * @param dstSw destination switch
	 * @param dstPort destination port
	 * @return the path with the best throughput.
	 */
	public Route getRouteByBandwith(long srcSw, short srcPort, long dstSw,
			short dstPort);
	

	 
	
}
