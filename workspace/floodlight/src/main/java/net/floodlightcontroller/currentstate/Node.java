package net.floodlightcontroller.currentstate;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.routing.Link;


/**
 * Object representation of a node (switch) in the topology 
 * @author goncaloSemedo@FCUL
 *
 */
public class Node implements Cloneable {



	private IOFSwitch sw;
	private LinkedList<Node> neighbours;
	private HashMap<Long, Link> neighboursLink;
	private HashMap<Long, Long> neighboursCost;
	private HashMap<Long, Double> neighboursBandwith;
	private long controllerRT;



	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();



	public Node(IOFSwitch sw, ILinkDiscoveryService linkDiscovery){

		this.sw = sw;
		this.controllerRT = 0;

		this.neighboursLink = new HashMap<Long, Link>();
		this.neighboursCost = new HashMap<Long, Long>();
		this.neighboursBandwith = new HashMap<Long, Double>();

		this.neighbours = new LinkedList<Node>();
		
	


	}
	
/**
 * Adds rtt between this node and the controller
 * @param rt round trip time
 */
	public void setControllerRt(Long rt){
		lock.writeLock().lock();
		this.controllerRT = rt;
		lock.writeLock().unlock();
	}

/**
 * rtt between this node and the controller
 * @return the rtt between this node and the controller
 */
	public long getControllerRt(){
		lock.readLock().lock();
		long rtt = this.controllerRT;
		lock.readLock().unlock();
		return rtt;
	}

/**
 * Adds the throughput between this node and a neighbor node
 * @param dpid dpid of the neighbor node
 * @param bandwith the throughput value
 */
	public void addNeighbourBandwith(long dpid, double bandwith){
		
		lock.writeLock().lock();

		neighboursBandwith.put(dpid, bandwith);
		
		lock.writeLock().unlock();

	}

	/**
	 * Adds a link and respective latency between this node and a 
	 * neighbor node
	 * @param l the link
	 * @param latency
	 */
	public void addNeighbourLink(Link l, long latency){

		lock.writeLock().lock();
		for(Node n : neighbours){
			if(n.getSwitchDPID() == l.getDst()){
				neighboursLink.put(n.getSwitchDPID(), l);
				neighboursCost.put(n.getSwitchDPID(), latency);
				lock.writeLock().unlock();
				return;
			}
		}
		lock.writeLock().unlock();


	}

/**
 * The latency value between this node and a neighbor node
 * @param neighbour: neighbor node dpid
 * @return The latency value between this node and a neighbor node
 */
	public Long getNeighbourCost(long neighbour){
		lock.readLock().lock();
		long cost = neighboursCost.get(neighbour);
		lock.readLock().unlock();
		return cost;

	}

	/**
	 * The link between this node and a neighbor node.
	 * @param neighbour: neighbor node dpid
	 * @return The link between this node and a neighbor node.
	 */
	public Link getNeighbourLink(long neighbour){

		lock.readLock().lock();
		Link l = neighboursLink.get(neighbour);
		lock.readLock().unlock();
		return l;
	}
	

/**
 * The trouhgput between this node and a neighbor node.
 * @param neighbour
 * @return  The trouhgput between this node and a neighbor node.
 */
	public double getNeighbourBandwith(long neighbourdpid){
		
		lock.readLock().lock();
		double band = neighboursBandwith.get(neighbourdpid);
		lock.readLock().unlock();
		return band;
	}


	/**
	 * The list of neighbors of this node
	 * @return The list of neighbors of this node
	 */
	public LinkedList<Node> getNeighbours(){
		return neighbours;
	}

	/**
	 * Add a new neighbor to this node
	 * @param n the neighbor node
	 */
	public void addNeighbour(Node n){

		neighboursBandwith.put(n.getSwitchDPID(),  20.0);
		neighbours.add(n);

	}

	/**
	 * The switch represented by this object
	 * @return The switch represented by this object
	 */
	public IOFSwitch getSwitch(){
		return this.sw;
	}

	/**
	 * The DPID of the represented switch by this object
	 * @return The DPID of the represented switch by this object
	 */
	public long getSwitchDPID(){

		return this.sw.getId();
	}


}
