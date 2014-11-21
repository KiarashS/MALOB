package net.floodlightcontroller.currentstate;

import java.util.LinkedList;


import org.openflow.util.HexString;

import net.floodlightcontroller.routing.RouteId;

/**
 * Object that represents a Path
 * @author GoncaloSemedo@FCUL
 *
 */
public class Path {

	private RouteId id;
	private LinkedList<Node> path;
	
	public Path(RouteId id,LinkedList<Node> path){
		
		this.id = id;
		this.path = new LinkedList<Node>();
		this.path = path;
	
	}
	

	/**
	 * The id of the Path in the RouteId object format
	 * @return The id of the Path in the RouteId object format
	 */
	public RouteId getId(){
		return this.id;
	}
	
	/**
	 * Return the list of all nodes that belong to a path
	 * @return the list of all nodes that belong to a path
	 */
	public LinkedList<Node> getPath(){
		
		return this.path;
	}
	
	
	
	public String toString(){
		StringBuilder builder = new StringBuilder();
		System.out.println("path sixze: "+ path.size());
		
		builder.append("Src: "+id.getSrc()+" DSt: "+id.getDst());
		builder.append("\n");
		for(Node n: path)
			builder.append(" --> " +HexString.toHexString(n.getSwitchDPID()));
		
		
		return builder.toString();
		
	}
}
