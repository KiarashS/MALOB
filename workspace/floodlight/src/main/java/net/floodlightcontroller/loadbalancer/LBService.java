package net.floodlightcontroller.loadbalancer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * DAta structure that represents a service
 * @author GoncaloSemedo@FCUL
 *
 */
@JsonSerialize(using=LBServiceSerializer.class)
public class LBService {

	protected String serviceName;
	protected Short port;
	protected int algorithm;
		
	protected static final int SPS_ALGORITHM = 0;
	protected static final int BBPS_ALGORITHM = 1;
	protected static final int RT_ALGORITHM = 2;
	
	public LBService (){
		
		serviceName = null;
		port = 0;
		algorithm = 0;

	}
	
	
	
	public String toString(){
		
		String algo = null;
		
		switch (this.algorithm) {
		
		case 0:
			algo = "Shortest Latency Path Server";  
			break;
		case 1:
			algo = "Highest Throughput Path Server";  
			break;	
		case 2:
			algo = "CPU usage";  
			break;
			
		default:
			algo = "Shortest Latency Path Server"; 
			break;
		}
		
		return serviceName + " Port:"+port+" Algortithm: "+algo;
	}
	
	

}
