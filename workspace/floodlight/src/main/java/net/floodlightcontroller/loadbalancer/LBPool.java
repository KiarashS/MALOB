/**
 *    Copyright 2013, Big Switch Networks, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.loadbalancer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.loadbalancer.LoadBalancer.IPClient;
import net.floodlightcontroller.packet.IPv4;


/**
 * Data structure for Load Balancer based on
 * Quantum proposal http://wiki.openstack.org/LBaaS/CoreResourceModel/proposal 
 * 
 * @author KC Wang
 */


@JsonSerialize(using=LBPoolSerializer.class)
public class LBPool {
	protected String id;
	protected String name;
	protected String tenantId;
	protected String netId;
	protected short lbMethod;
	protected byte protocol;
	protected ArrayList<String> members;
	protected ArrayList<String> monitors;
	protected short adminState;
	protected short status;

	protected String vipId;

	protected int previousMemberIndex;

	public LBPool() {
		id = String.valueOf((int) (Math.random()*10000));
		name = null;
		tenantId = null;
		netId = null;
		lbMethod = 0;
		protocol = 0;
		members = new ArrayList<String>();
		monitors = new ArrayList<String>();
		adminState = 0;
		status = 0;
		previousMemberIndex = -1;
	}

	public String pickMember(net.floodlightcontroller.loadbalancer.MALOB.IPClient client) {
		// simple round robin for now; add different lbmethod later
		if (members.size() > 0) {
			previousMemberIndex = (previousMemberIndex + 1) % members.size();
			return members.get(previousMemberIndex);
		} else {
			return null;
		}
	}

	/**
	 * An algorithm that selects the server base on the Response Time of the servers
	 * (not in use)
	 * @param members List of servers
	 * @return the server with the smallest response time
	 */
	public LBMember responseTime(HashMap<String, LBMember> members){

		long bestRT = Long.MAX_VALUE;
		LBMember target = null;
		String bestId = null;
		
		LinkedList<String> bestTargets = new LinkedList<String>();

		for(String id : members.keySet()){



			if( (members.get(id).responseTime + members.get(id).new_request_rt_impact) < bestRT){
				
				bestTargets.removeAll(bestTargets);
				bestTargets.add(id);

				bestRT = members.get(id).responseTime + members.get(id).new_request_rt_impact;
				bestId = id;
				
			}else if( (members.get(id).responseTime + members.get(id).new_request_rt_impact) == bestRT)
				bestTargets.add(id);
			
			
		}
		
		if(bestTargets.size() > 1){
			
			HashMap<String, LBMember> aux = new HashMap<String, LBMember>();
			
			for(String id : bestTargets)
				aux.put(id, members.get(id));
			
			target = this.cpuUsage(aux);
			
			
		}else
			target = members.get(bestId);

			members.get(target.id).responseTime += members.get(target.id).new_request_rt_impact;

		return target;
	}

	/**
	 * An algorithm that selects the server base on the number of connections of the servers
	 * (not in use)
	 * @param members List of servers
	 * @return the server with the least number of connections
	 */
	public LBMember leastConnections(HashMap<String, LBMember> members){

		LBMember target = null;

		int best = Integer.MAX_VALUE;
		String bestId = null;
		for(String id : members.keySet()){

			/*	if(members.get(id).address == IPv4.toIPv4Address("192.168.9.11"))
				members.get(id).nConnections = 6;
			 */

			if(members.get(id).nConnections < best){
				best = members.get(id).nConnections;
				target = members.get(id);
				bestId = id;
			}

		}

		members.get(bestId).nConnections++;

		return target;
	}

	/**
	 * An algorithm that selects the server base on CPU usage of the servers
	 * @param members List of servers
	 * @return the server with the lowest CPU usage
	 */
	public LBMember cpuUsage(HashMap<String, LBMember> members){

		LBMember target = null;

		double best = Double.MAX_VALUE;
		String bestId = null;

		for(String id : members.keySet()){

			if((members.get(id).cpuUsage + members.get(id).new_request_cpu_impact) < best){
				best = members.get(id).cpuUsage + members.get(id).new_request_cpu_impact;
				target = members.get(id);
				bestId = id;
			}

		}
		
		members.get(bestId).cpuUsage += members.get(bestId).new_request_cpu_impact;


		return target;
	}


}
