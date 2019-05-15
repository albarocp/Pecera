package fishkiller;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;

public class VertxMqttServer{
	
	 public void iniciarServidor(Vertx vertx) {
		 
		 MqttServer mqttServer = MqttServer.create(vertx);
		    mqttServer.endpointHandler(endpoint -> {
		    	
		      endpoint.accept(false);

		    }).listen(ar -> {

		        if (ar.succeeded()) {

		          System.out.println("MQTT a iniciado correctamente, puerto: " + ar.result().actualPort());
		        } else {

		          System.out.println("Error al iniciar MQTT");
		          ar.cause().printStackTrace();
		        }
		      });
	 }

	 public void cerrarConexionCliente(MqttEndpoint endpoint) {

		  
		    endpoint.disconnectHandler(v -> {

		      System.out.println("Se ha desconectado correctamente ");
		    });
	}
	 
	public void subscribirClienteATopic(MqttEndpoint endpoint) {

		    endpoint.subscribeHandler(subscribe -> {

		      List<MqttQoS> grantedQosLevels = new ArrayList<>();
		      for (MqttTopicSubscription s: subscribe.topicSubscriptions()) {
		    	  
		        System.out.println("Se ha suscrito al topic " + s.topicName() + " con QoS " + s.qualityOfService());
		        grantedQosLevels.add(s.qualityOfService());
		      }
		      //respuesta que se le envia al cliente(ack)
		      endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);

		    });
	}
	
	public void desuscribirClienteDeTopic(MqttEndpoint endpoint) {
		
		endpoint.unsubscribeHandler(unsubscribe -> {

			for (String t: unsubscribe.topics()) {
				
			    System.out.println("desuscrito de " + t);
			 }
			 //respuesta que se le envia al cliente(ack)
			 endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
		});
	}
	
	// Los niveles de QoS permiten saber el tipo de entrega que se realizará:
	//1 - AT_LEAST_ONCE: Se asegura que los mensajes llegan a los clientes, pero no
	// que se haga una única vez (pueden llegar duplicados)
	// 2 - EXACTLY_ONCE: Se asegura que los mensajes llegan a los clientes un única
	// vez (mecanismo más costoso)
	// 0 - AT_MOST_ONCE: No se asegura que el mensaje llegue al cliente, por lo que no
	// es necesario ACK por parte de éste
	
	/* Si se publica un mensaje en un topic, este metodo se encarga de que el mensaje publicado llegue a
	   todos los clientes suscritos a ese topic */
	public void enviarMensajePublicadoPorCliente(MqttEndpoint endpoint) {
		
		endpoint.publishHandler(message -> {

			  System.out.println("Mensaje publicado [" + message.payload().toString(Charset.defaultCharset()) + "] con QoS [" + message.qosLevel() + "]");

			  if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
				  
			    endpoint.publishAcknowledge(message.messageId());
			    
			  } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
			    endpoint.publishReceived(message.messageId());
			  }

			}).publishReleaseHandler(messageId -> {

			  endpoint.publishComplete(messageId);
		});
		
	}
	
	/*  */
	public void confirmacionClienteMensajeOk(MqttEndpoint endpoint) {
		
		endpoint.publish("topic_servidor", Buffer.buffer("hola desde el servidor MQTT Vert.x"), MqttQoS.EXACTLY_ONCE, false, false);

		
		endpoint.publishAcknowledgeHandler(messageId -> {
				//QoS 1
			System.out.println("ACK enviado del cliente del mensaje = " +  messageId);
		}).publishReceivedHandler(messageId -> {
			//QoS 2
			endpoint.publishRelease(messageId);
		}).publishCompletionHandler(messageId -> {
			
			System.out.println("ACK enviado del cliente del mensaje = " +  messageId);
		});
		
	}
	
	 public void cerrarServidorMqtt(MqttServer mqttServer) {

		 mqttServer.close(v -> {

			 System.out.println("servidor MQTT se ha cerrado correctamente");
		 });
	 }
	 

}
