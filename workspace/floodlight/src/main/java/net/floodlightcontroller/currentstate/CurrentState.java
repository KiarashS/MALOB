package net.floodlightcontroller.currentstate;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import java.util.Collection;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;


import org.openflow.protocol.OFMessage;

import org.openflow.protocol.OFType;

import org.openflow.protocol.factory.BasicFactory;



import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.Link;

import net.floodlightcontroller.MALOB.ILoadBalancerService;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.counter.ICounterStoreService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;

import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;


import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.routing.RouteId;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.util.OFMessageDamper;





/**
 * This class has the load balancing algorithms used by MALOB
 * 
 * @author GoncaloSemedo@FCUL
 *
 */
public class CurrentState implements IFloodlightModule,ICurrentStateService, IDeviceListener {

	protected IFloodlightProviderService floodlightProvider;
	protected ITopologyService topology;
	protected ILinkDiscoveryService linkDiscovery;
	protected IRoutingService rService;
	protected IDeviceService deviceService;
	protected OFMessageDamper damper;
	protected ICounterStoreService counterStore;
	protected ILoadBalancerService lbService;

	protected PathLB pathLB;

	private HashMap< RouteId, LinkedList<Path> > pathsMap;
	private LinkedList<IDevice> deviceList;
	private LinkedList<Node> nodeList;


	public static final int VIP = IPv4.toIPv4Address("192.168.9.100");

	public static HashMap<Long, Long> controllerLatency = new HashMap<Long, Long>();

	public static HashMap<Long, Long> switchTS = new HashMap<Long, Long>();

	public static ReentrantLock lock = new ReentrantLock();

	
/**
 * Method used by the controller to add the round-trip time packets of packets 
 * between switches and the controller
 * @param sw: the switch
 * @param end: Time when the round trip ended. 
 */
	public static synchronized void addControllerRTT(long sw, long end){

		long start = switchTS.get(sw);

		long finalT = (end - start);

		controllerLatency.put(sw, finalT);


	}

	/**
	 * Creates a new node when a new device is found
	 * @param newDevice the new device founded
	 */
	private synchronized void createInstance(IDevice newDevice){

		if(deviceList.size()  == 1){


			Set<Link> links = linkDiscovery.getLinks().keySet();

			for(long dpid : floodlightProvider.getAllSwitchDpids()){
				nodeList.add(new Node(floodlightProvider.getSwitch(dpid),
						linkDiscovery));

			}
			addNeighbours(links);

			return;
		} 




		long attachedSwitch = newDevice.getAttachmentPoints()[0].getSwitchDPID();

		for(RouteId id: pathsMap.keySet()){

			if((id.getDst() == attachedSwitch) || (id.getSrc() == attachedSwitch))
				return;

		}




		// discovers all possible routes between the new device and the other discovered devices. 
		for(int i = 0; i<deviceList.size()-1; i++){
			getAllRoutes(deviceList.get(i), newDevice);

		}


	}


/**
 * discovers all possible routes between two different devices.
 * @param device first device
 * @param newDevice second device
 */
	private void  getAllRoutes(IDevice device, IDevice newDevice) {
		// TODO Auto-generated method stub
		Node src = null;
		Node dst = null;

		for(Node n : nodeList){

			if(device.getAttachmentPoints()[0].getSwitchDPID() == n.getSwitchDPID())
				src = n;
			else if (newDevice.getAttachmentPoints()[0].getSwitchDPID() == n.getSwitchDPID())
				dst = n;
		}

		LinkedList<Node> pathsList = new LinkedList<Node>();

		discoverPaths(src , dst, pathsList);

	}


/**
 * Recursive method to discovers all possible routes between two different devices.
 * @param src The first node
 * @param dst the destination node
 * @param pathsList list of nodes found that belong to a certain path
 */
	private void discoverPaths(Node src, Node dst, LinkedList<Node> pathsList) {
		// TODO Auto-generated method stub

		if(pathsList.contains(src))
			return;

		pathsList.add(src);

		if(pathsList.contains(dst)){
			createNewPath(pathsList);
			pathsList.removeLast();
			return;
		}

		for(Node nb : src.getNeighbours())
			discoverPaths(nb, dst, pathsList);

		pathsList.removeLast();
	}	

	/**
	 * Creates a new Path
	 * @param pathsList the list of all nodes that belong to a certain path 
	 */
	private void createNewPath(LinkedList<Node> pathsList) {
		// TODO Auto-generated method stub

		RouteId id = new RouteId(pathsList.getFirst().getSwitchDPID(), pathsList.getLast().getSwitchDPID());

		LinkedList<Node> list = new LinkedList<Node>();

		for(Node n : pathsList)
			list.add(n);

		Path path = new Path(id, list);

		if(!pathsMap.containsKey(id))
			pathsMap.put(id, new LinkedList<Path>());

		pathsMap.get(id).add(path);


		//reverse

		RouteId reverseId = new RouteId(pathsList.getLast().getSwitchDPID(),pathsList.getFirst().getSwitchDPID());

		LinkedList<Node> reverseList = new LinkedList<Node>();

		for(Node n : pathsList)
			reverseList.add(n);

		Collections.reverse(reverseList);


		Path reversePath = new Path(reverseId, reverseList);

		if(!pathsMap.containsKey(reverseId))
			pathsMap.put(reverseId, new LinkedList<Path>());

		pathsMap.get(reverseId).add(reversePath);

	}


	/**
	 * Creates a list of neighbours on all nodes
	 * @param links from the ILinkDiscoveryService used to create the neighbours lists.
	 */
	private void addNeighbours( Set<Link> links) {

		for(Node n: nodeList ){
			for(Link l: links){
				if(l.getSrc() == n.getSwitchDPID()){
					for(int i = 0; i<nodeList.size();i++)
						if(nodeList.get(i).getSwitchDPID() == l.getDst()){
							n.addNeighbour(nodeList.get(i));
							i = nodeList.size();
						}
				}

			}

		}




	}

	/**
	 * Converts from a Path format to a Route format
	 * @param Path path:
	 * @return LinkedList<NodePortTuple> used to create a Route 
	 */
	private LinkedList<NodePortTuple> convetToRoute(Path path){

		LinkedList<NodePortTuple> nptList = new LinkedList<NodePortTuple>();
		LinkedList<Node> route = path.getPath();



		for(int i = 0; i<route.size()-1; i++){

			Link l = route.get(i).getNeighbourLink(route.get(i+1).getSwitchDPID());

			nptList.add( new NodePortTuple(l.getSrc(), l.getSrcPort()) );
			nptList.add( new NodePortTuple(l.getDst(), l.getDstPort()) );
		}

		return nptList;
	}


	/**
	 * Converts from Route to Path
	 * @param Route route
	 * @return Path path
	 */
	private Path convertToPath(Route route){


		long swId = 0;

		LinkedList<Node> path = new LinkedList<Node>();

		for(NodePortTuple npt : route.getPath()){
			if(swId != npt.getNodeId()){
				swId = npt.getNodeId();
				for(int i = 0; i<nodeList.size(); i++){
					if(nodeList.get(i).getSwitchDPID() == swId){
						path.add(nodeList.get(i));
						i = nodeList.size();
					}
				}

			}

		}


		return new Path(route.getId(), path);
	}


	
	/**
	 * Adds the round-trip time of packet between 
	 * switches and the controller
	 */
	private void addNodeControllerRT(){

		for(Node n: nodeList){
			n.setControllerRt(controllerLatency.get(n.getSwitchDPID()));
		}

	}

	@Override
	public void addNeigbourLink(Link l, long latency){


		for(Node n: nodeList){

			if(n.getSwitchDPID() == l.getSrc()){
				n.addNeighbourLink(l, latency);
				return;
			}
		}
	}



	@Override
	public void handleBandwithInfo(Data data){

		String info = null;


		try {
			info = new String(data.getData(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		String [] aux = info.split(":");

		int src = IPv4.toIPv4Address(aux[0]);
		int dst = IPv4.toIPv4Address(aux[1]);
		double bandwith = Double.parseDouble(aux[2]);

		long srcDPID = 0;
		long dstDPID = 0;


		

		for(IDevice device : deviceList){

			if(device.getIPv4Addresses()[0] == src){
				srcDPID = device.getAttachmentPoints()[0].getSwitchDPID();

			}else if (device.getIPv4Addresses()[0] == dst){
				dstDPID = device.getAttachmentPoints()[0].getSwitchDPID();
			}

		}

		if(srcDPID == 0){

			for(long sw : floodlightProvider.getAllSwitchDpids()){

				if(!hasAttachement(sw))
					srcDPID = sw;

			}
		}

		if(dstDPID == 0){

			for(long sw : floodlightProvider.getAllSwitchDpids()){

				if(!hasAttachement(sw))
					dstDPID = sw;

			}
		}


		for(Node n : nodeList){

			if(n.getSwitchDPID() == srcDPID)
				n.addNeighbourBandwith(dstDPID, bandwith);

		}

	}

/**
 * Verifies if a switch have hosts attached
 * @param sw the switch
 * @return true if have hosts attached, false otherwise.
 */
	private boolean hasAttachement(long sw){


		for(IDevice d: deviceList){

			if(d.getAttachmentPoints()[0].getSwitchDPID() == sw)
				return true;

		}

		return false;

	}

	@Override
	public long getRouteLatency(Route route){

		pathLB.setLinkLatencies(nodeList);
		long latency = 0;

		Path p = convertToPath(route);
		latency = pathLB.getTotalPathLatency(p);

		latency += (p.getPath().getFirst().getControllerRt()/2);
		latency += (p.getPath().getLast().getControllerRt()/2);

		return latency;

	}


	@Override
	public Route getRoute(long srcSw, short srcPort, long dstSw, short dstPort) {
		// TODO Auto-generated method stub

		addNodeControllerRT();
		pathLB.setLinkLatencies(nodeList);

		Node src = null;
		Node dst = null;



		for(Node n: nodeList){
			if(n.getSwitchDPID() == srcSw)
				src = n;
			else if(n.getSwitchDPID() == dstSw)
				dst = n;


		}


		Path path = pathLB.minimumLatencyPath(src, dst);

		RouteId id = new RouteId(srcSw, dstSw);

		NodePortTuple nptSrc = new NodePortTuple(srcSw, srcPort);
		NodePortTuple nptDst = new NodePortTuple(dstSw, dstPort);
		LinkedList<NodePortTuple> nptList;

		nptList = convetToRoute(path);
		nptList.addFirst(nptSrc);
		nptList.addLast(nptDst);

		Route route = new Route(id, nptList);


		return route;

	}
	
	
	
	@Override
	public Route getRouteByBandwith(long srcSw, short srcPort, long dstSw, short dstPort){

		Route route = null;

		RouteId id = new RouteId(srcSw, dstSw);

		double bestBandwith = 0;
		Path bestPath = null;
		double pathBand = 0;

		for(Path p : pathsMap.get(id)){

			pathBand = getMaximumBand(p);

			if(bestBandwith < pathBand){
				bestPath = p;
				bestBandwith = pathBand;
			}
		}


		NodePortTuple nptSrc = new NodePortTuple(srcSw, srcPort);
		NodePortTuple nptDst = new NodePortTuple(dstSw, dstPort);


		LinkedList<NodePortTuple> nptList;

		nptList = convetToRoute(bestPath);

		nptList.addFirst(nptSrc);
		nptList.addLast(nptDst);

		route = new Route(id, nptList);


		return route;
	}


	/**
	 * Calculates the total Throughput of a path
	 * @param path: The path
	 * @return the total Throughput of the path
	 */
	private double getMaximumBand(Path path) {
		// TODO Auto-generated method stub


		double smallerBand = Double.MAX_VALUE;

		for(int i = 0; i<path.getPath().size()-1 ; i++){

			if( smallerBand > (path.getPath().get(i).getNeighbourBandwith(path.getPath().get(i+1).getSwitchDPID())) ){

				smallerBand = (path.getPath().get(i).getNeighbourBandwith(path.getPath().get(i+1).getSwitchDPID()));
			}

		}

		return smallerBand;
	}


	@Override
	public Route getServerRoutebyBandwith(long srcSw, short srcPort,long dstSw1,short dstPort1,
			ArrayList<Integer> members) {


		Route bestRoute = null;
		double bestBand = 0;


		for(int ip : members){

			for(IDevice d : deviceList){

				if(d.getIPv4Addresses()[0] == ip){

					Route r = this.getRouteByBandwith(
							srcSw, 
							srcPort, 
							d.getAttachmentPoints()[0].getSwitchDPID(), 
							(short) d.getAttachmentPoints()[0].getPort());		

					double rBand = this.getMaximumBand(convertToPath(r));

					if(rBand > bestBand){
						bestRoute = r;
						bestBand = rBand;
					}

				}

			}

		}



		return bestRoute;

	}


	@Override
	public Route getServerRoute(long srcSw, short srcPort,long dstSw1,short dstPort1,
			ArrayList<Integer> members) {

		addNodeControllerRT();
		pathLB.setLinkLatencies(nodeList);

		short portDst = 0;

		Node src = null;
		Node dst = null;

		for(Node n: nodeList){
			if(n.getSwitchDPID() == srcSw){
				src = n;
				
			}
		}



		Path path = pathLB.superLB(src, members, deviceList);

		dst = path.getPath().getLast();


		for(int i = 0; i<deviceList.size() ; i++){

			if(deviceList.get(i).getAttachmentPoints()[0].getSwitchDPID() == 
					dst.getSwitchDPID()){
				portDst = (short) deviceList.get(i).getAttachmentPoints()[0].getPort();
				i = deviceList.size();
			}

		}

		long swDst = dst.getSwitchDPID();


		RouteId id = new RouteId(srcSw, swDst);



		NodePortTuple nptSrc = new NodePortTuple(srcSw, srcPort);
		NodePortTuple nptDst = new NodePortTuple(swDst, portDst);


		LinkedList<NodePortTuple> nptList;

		nptList = convetToRoute(path);

		nptList.addFirst(nptSrc);
		nptList.addLast(nptDst);

		Route route = new Route(id, nptList);




		return route;
	}



	@Override
	public void getAllControllerRtt(FloodlightContext cntx){

		OFMessage m = BasicFactory.getInstance().getMessage(OFType.ECHO_REQUEST);



		m.setXid(777);

		for(long s : floodlightProvider.getAllSwitchDpids()){

			IOFSwitch sw = floodlightProvider.getSwitch(s);
			long time =System.currentTimeMillis();
			switchTS.put(s, time);


			try {

				damper.write(sw, m, cntx);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}




	}






	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(ICurrentStateService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(ICurrentStateService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(ITopologyService.class);
		l.add(ILinkDiscoveryService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub


		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		topology = context.getServiceImpl(ITopologyService.class);
		linkDiscovery = context.getServiceImpl(ILinkDiscoveryService.class);
		rService = context.getServiceImpl(IRoutingService.class);
		deviceService = context.getServiceImpl(IDeviceService.class);
		counterStore = context.getServiceImpl(ICounterStoreService.class);
		lbService = context.getServiceImpl(ILoadBalancerService.class);

		pathsMap = new HashMap< RouteId, LinkedList<Path> >();
		deviceList = new LinkedList<IDevice>();
		nodeList = new LinkedList<Node>();


		damper =  new OFMessageDamper(10000,
				EnumSet.of(OFType.ECHO_REQUEST),
				250);


		pathLB = new PathLB();


		/*	try {
			readServerMacsFromFile();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/


		for(long s: floodlightProvider.getAllSwitchMap().keySet()){

			addControllerRTT(s, (long) 0);

		}




	}

	@Override
	public LinkedList<IDevice> getDeviceList(){
		return this.deviceList;
	}




	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {

		deviceService.addListener(this);	

	}



	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return CurrentState.class.getSimpleName();
	}



	@Override
	public boolean isCallbackOrderingPrereq(String type, String name) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public boolean isCallbackOrderingPostreq(String type, String name) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public void deviceAdded(IDevice device) {
		// TODO Auto-generated method stub

		if(device.getIPv4Addresses().length == 0)
			return;


		deviceList.add(device);

		createInstance(device);
	}



	@Override
	public void deviceRemoved(IDevice device) {
		// TODO Auto-generated method stub

	}



	@Override
	public void deviceMoved(IDevice device) {
		// TODO Auto-generated method stub

	}



	@Override
	public void deviceIPV4AddrChanged(IDevice device) {
	
		if(!deviceList.contains(device)){

			deviceList.add(device);

			createInstance(device);
		}

	}



	@Override
	public void deviceVlanChanged(IDevice device) {
		// TODO Auto-generated method stub

	}




















}
