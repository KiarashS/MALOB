package net.floodlightcontroller.currentstate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;



import net.floodlightcontroller.currentstate.Node;
import net.floodlightcontroller.currentstate.Path;
import net.floodlightcontroller.devicemanager.IDevice;


import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.RouteId;


/**
 * The Load Balancing algorithms for path selection 
 * @author GoncaloSemedo@FCUL
 *
 */
public class PathLB {

	private HashMap<Link, Long> linkLatencies;
	private Map<Node, Long> distance;
	private Map<Node, Node> predecessors;
	private LinkedList<Node> visitedNodes ;
	private LinkedList<Node> unVisitedNodes;
	private LinkedList<Node> nodeList;

	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public PathLB(){

		linkLatencies = new HashMap<Link, Long>();


	}

	/**
	 * e latency between nodes, removing the controller rrt.
	 * @param nodeList the list of nodes
	 */
	public void setLinkLatencies(LinkedList<Node> nodeList){


		lock.writeLock().lock();

		this.nodeList = nodeList;

		for(Node n: nodeList){
			long crt = n.getControllerRt();
			for(Node nb : n.getNeighbours()){
				long crt2 = nb.getControllerRt();

				Link l = n.getNeighbourLink(nb.getSwitchDPID());
				long latency = n.getNeighbourCost(nb.getSwitchDPID());

				long trueLat =  (latency - ((crt + crt2)/2));

				linkLatencies.put(l, trueLat);

			}

		}
		lock.writeLock().unlock();


	}

	/**
	 * Discovers the paths with the lowest latency to all servers
	 * @param src source node
	 * @param members list of members (servers)
	 * @param deviceList list of all devices 
	 * @return The path with the lowest latency among all the discovered paths.
	 */
	public Path superLB (Node src, ArrayList<Integer> members, LinkedList<IDevice> deviceList){


		long pathLatency = 0;
		Node dst = null;
		Path bestPath = null;
		long bestLat = Long.MAX_VALUE;
		int count = 0;
		Path path = null;

		for(int ip : members){
			for(IDevice device : deviceList){

				if(device.getIPv4Addresses()[0] == ip){
					for(int i = 0; i<nodeList.size();i++){
						if(nodeList.get(i).getSwitchDPID() == 
								device.getAttachmentPoints()[0].getSwitchDPID()){
							dst = nodeList.get(i);
							i = nodeList.size();
						}
					}

				}

			}

			if(count == 0)
				path = minimumLatencyPath(src, dst);
			else
				path = getPath(dst);

			count++;

			if(path != null){

				pathLatency = getTotalPathLatency(path);
				
				if(pathLatency < bestLat){

					bestLat = pathLatency;
					bestPath = path;

				}

			}
		}



		return bestPath;
	}




	/**
	 * Computes the lowest latency path to a server
	 * using Dijkstra’s algorithm.
	 * @param src source Node
	 * @param dst destination Node
	 * @return the lowest latency path to a server
	 */
	public Path minimumLatencyPath(Node src, Node dst) {
		lock.writeLock().lock();

		Node dstSw = dst;


		distance = new HashMap<Node, Long>();
		predecessors = new HashMap<Node, Node>();

		visitedNodes = new LinkedList<Node>();
		unVisitedNodes = new LinkedList<Node>();

		unVisitedNodes.add(src);


		distance.put(src, (long) 0);



		while(!unVisitedNodes.isEmpty()){

			Node node = getMinimum(unVisitedNodes);
			visitedNodes.add(node);

			unVisitedNodes.remove(node);
			findMinimalDistances(node);

		}

		Path path = getPath(dstSw);
		lock.writeLock().unlock();



		return path;
	}

	/**
	 * Auxiliary method of the Dijkstra’s algorithm
	 * @param unVisitedNodes
	 * @return
	 */
	private Node getMinimum(LinkedList<Node> unVisitedNodes) {

		Node minimum = null;
		for (Node sw : unVisitedNodes) {
			if (minimum == null) {
				minimum = sw;
			} else {
				if (getShortestDistance(sw) < getShortestDistance(minimum)) {
					minimum = sw;
				}
			}
		}
		return minimum;
	}

	/**
	 * Auxiliary method of the Dijkstra’s algorithm
	 * @param unVisitedNodes
	 * @return
	 */
	private long getShortestDistance(Node swDst) {

		Long d = distance.get(swDst);
		if (d == null) {
			return Long.MAX_VALUE;
		} else {
			return d;
		}
	}

	/**
	 * Auxiliary method of the Dijkstra’s algorithm
	 * @param unVisitedNodes
	 * @return
	 */
	private void findMinimalDistances(Node node) {

		for (Node target : node.getNeighbours()) {
			if (getShortestDistance(target) > getShortestDistance(node)
					+ getDistance(node, target)) {
				distance.put(target, (getShortestDistance(node)
						+ getDistance(node, target)));
				predecessors.put(target, node);
				unVisitedNodes.add(target);
			}
		}

	}


	/**
	 * Auxiliary method of the Dijkstra’s algorithm
	 * @param unVisitedNodes
	 * @return
	 */
	private long getDistance(Node node, Node target) {
		lock.readLock().lock();
		Link l = node.getNeighbourLink(target.getSwitchDPID());
		long lat = linkLatencies.get(l);

		if(lat < 0){

			Link reverse = target.getNeighbourLink(node.getSwitchDPID());
			lat = linkLatencies.get(reverse);
		}

		if(lat < 0)
			lat = 10;

		lock.readLock().unlock();
		return lat;

	}

	/**
	 * The total latency of a path
	 * @param path
	 * @return The total latency of a path
	 */
	public long getTotalPathLatency(Path path){

		long latency = 0;

		for(int i = 0; i<path.getPath().size()-1; i++){

			latency += getDistance(path.getPath().get(i), path.getPath().get(i+1));
		}

		return latency;
	}

	/**
	 * The path to a target Node
	 * @param target Node
	 * @return The path to a target Node
	 */
	public Path getPath(Node target) {
		LinkedList<Node> path = new LinkedList<Node>();
		Node step = target;
		// check if a path exists
		if (predecessors.get(step) == null) {
			return null;
		}
		path.add(step);
		while (predecessors.get(step) != null) {
			step = predecessors.get(step);
			path.add(step);
		}
		// Put it into the correct order
		Collections.reverse(path);
		RouteId id = new RouteId(path.getFirst().getSwitchDPID(), path.getLast().getSwitchDPID());
		Path p = new Path(id, path);



		return p;
	}




}
