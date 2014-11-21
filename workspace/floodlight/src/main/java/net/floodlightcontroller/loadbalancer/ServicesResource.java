package net.floodlightcontroller.loadbalancer;

import java.io.IOException;
import java.util.Collection;

import net.floodlightcontroller.packet.IPv4;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class ServicesResource extends ServerResource {

	@Get("json")
	public Collection <LBService> retrieve() {
		ILoadBalancerService lbs =
				(ILoadBalancerService)getContext().getAttributes().
				get(ILoadBalancerService.class.getCanonicalName());

		String serviceName = (String) getRequestAttributes().get("service_name");
		if (serviceName!=null)
			return lbs.listService(serviceName);
		else
			return lbs.listServices();
	}


	@Put
	@Post
	public LBService createService(String postData) {

		LBService service=null;
		try {
			service=jsonToService(postData);
		} catch (IOException e) {
			System.err.println("Could not parse JSON : " + e.getMessage());
		}

		ILoadBalancerService lbs =
				(ILoadBalancerService)getContext().getAttributes().
				get(ILoadBalancerService.class.getCanonicalName());

		String serviceName = (String) getRequestAttributes().get("service_name");
		if (serviceName != null)
			return lbs.updateService(service);
		else
			return lbs.createService(service);
	}

	@Delete
	public int removeService() {

		String serviceName = (String) getRequestAttributes().get("service_name");

		ILoadBalancerService lbs =
				(ILoadBalancerService)getContext().getAttributes().
				get(ILoadBalancerService.class.getCanonicalName());

		return lbs.removeService(serviceName);
	}



	protected LBService jsonToService(String json) throws IOException {

		if (json==null) return null;

		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;
		LBService service = new LBService();

		try {
			jp = f.createJsonParser(json);
		} catch (JsonParseException e) {
			throw new IOException(e);
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new IOException("Expected START_OBJECT");
		}

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new IOException("Expected FIELD_NAME");
			}

			String n = jp.getCurrentName();
			jp.nextToken();
			if (jp.getText().equals("")) 
				continue;

			if (n.equals("service_name")) {
				service.serviceName = jp.getText();
				continue;
			} 


			if (n.equals("port")) {
				service.port = Short.parseShort(jp.getText());
				continue;
			}
			if (n.equals("algorithm")) {
				service.algorithm = Integer.parseInt(jp.getText());
				continue;
			}

		}
		jp.close();

		return service;
	}

}
