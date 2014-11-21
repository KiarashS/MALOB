package net.floodlightcontroller.loadbalancer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class LBServiceSerializer  extends JsonSerializer<LBService> {

	@Override
	public void serialize(LBService service, JsonGenerator jGen,
			SerializerProvider serializer) throws IOException,
			JsonProcessingException {
		// TODO Auto-generated method stub

		jGen.writeStartObject();

		jGen.writeStringField("service_name", service.serviceName);
		jGen.writeStringField("port", Short.toString(service.port));
		jGen.writeStringField("algorithm", Integer.toString(service.algorithm));
		jGen.writeEndObject();

	}

}
