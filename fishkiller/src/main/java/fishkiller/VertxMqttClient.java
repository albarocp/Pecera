package fishkiller;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;

public class VertxMqttClient {

	
	public void connectionTest(Vertx vertx) {
		
		 MqttClient client = MqttClient.create(vertx);

		 client.connect(1883, "dad.us", s -> {
		      client.disconnect();
		 });
	}
}
