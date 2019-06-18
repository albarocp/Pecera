package fishkiller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonParser;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttPublishMessage;

import io.vertx.mqtt.impl.MqttClientImpl;
public class VertxMqttServer extends AbstractVerticle {

	private static Multimap<String, MqttEndpoint> clientTopics;
	JsonObject message;
	public void start(Future<Void> startFuture) {
		clientTopics = HashMultimap.create();
		// Configuramos el servidor MQTT
		MqttServer mqttServer = MqttServer.create(vertx);
		init(mqttServer);

		// Creamos un cliente de prueba para MQTT que publica mensajes cada 3 segundos
		MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));

		/*
		 * Nos conectamos al servidor que est� desplegado por el puerto 1883 en la
		 * propia m�quina. Recordad que localhost debe ser sustituido por la IP de
		 * vuestro servidor. Esta IP puede cambiar cuando os desconect�is de la red, por
		 * lo que aseguraros siempre antes de lanzar el cliente que la IP es correcta.
		 */
		mqttClient.connect(1883, "localhost", s -> {

			/*
			 * Nos suscribimos al topic_2. Aqu� debera indicar el nombre del topic al que os
			 * quer�is suscribir. Adem�s, pod�is indicar el QoS, en este caso AT_LEAST_ONCE
			 * para asegurarnos de que el mensaje llega a su destinatario.
			 */
			mqttClient.subscribe("topic_server", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
				if (handler.succeeded()) {
					/*
					 * En este punto el cliente ya est� suscrito al servidor, puesto que se ha
					 * ejecutado la funci�n de handler
					 */
					System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal topic_server");

					/*
					 * Adem�s de suscribirnos al servidor, registraremos un manejador para
					 * interceptar los mensajes que lleguen a nuestro cliente. De manera que el
					 * proceso ser�a el siguiente: El cliente exterbi env�a un mensaje al servidor
					 * -> el servidor lo recibe y busca los clientes suscritos al topic -> el
					 * servidor reenv�a el mensaje a esos clientes -> los clientes (en este caso el
					 * cliente actual) recibe el mensaje y lo procesa si fuera necesario.
					 */
					mqttClient.publishHandler(new Handler<MqttPublishMessage>() {
						@Override
						public void handle(MqttPublishMessage arg0) {
							/*
							 * Si se ejecuta este c�digo es que el cliente 2 ha recibido un mensaje
							 * publicado en alg�n topic al que estaba suscrito (en este caso, al topic_2).
							 */
							message = new JsonObject(arg0.payload());
							System.out.println("-----" + message.getString("clientId"));
							System.out.println("-----" + mqttClient.clientId());
							if (!message.getString("clientId").equals(mqttClient.clientId()))
								System.out.println("Mensaje recibido por el cliente: " + arg0.payload().toString());
						}
					});
				}
			
			});

			/*
			 * Este timer env�a mensajes desde el cliente al servidor cada 3 segundos.
			 */
			
			
			new Timer().scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					/*
					 * Publicamos un mensaje en el topic "topic_2"
					 */

					Random random = new Random();
					if (mqttClient.isConnected()) {
					
							
								mqttClient.publish("topic_server",
								Buffer.buffer(new JsonObject(String.valueOf(message)).encode()),
								MqttQoS.AT_LEAST_ONCE, false, false);
								
						/*
						mqttClient.publish("topic_server",
								Buffer.buffer(new JsonObject().put("action", random.nextInt(2) == 0 ? "on" : "off")
										.put("timestamp", Calendar.getInstance().getTimeInMillis())
										.put("clientId", mqttClient.clientId()).encode()),
								MqttQoS.AT_LEAST_ONCE, false, false);*/
						
					}
				}
			}, 3000, 3000);
		});
	}

	/**
	 * M�todo encargado de inicializar el servidor y ajustar todos los manejadores
	 * 
	 * @param mqttServer
	 */
	private static void init(MqttServer mqttServer) {
		mqttServer.endpointHandler(endpoint -> {
			/*
			 * Si se ejecuta este c�digo es que un cliente se ha suscrito al servidor MQTT
			 * para alg�n topic.
			 */
			System.out.println("Nuevo cliente MQTT [" + endpoint.clientIdentifier()
					+ "] solicitando suscribirse [Id de sesi�n: " + endpoint.isCleanSession() + "]");
			/*
			 * Indicamos al cliente que se ha contectado al servidor MQTT y que no ten�a
			 * sesi�n previamente creada (par�metro false)
			 */
			endpoint.accept(false);

			/*
			 * Handler para gestionar las suscripciones a un determinado topic. Aqu�
			 * registraremos el cliente para poder reenviar todos los mensajes que se
			 * publicen en el topic al que se ha suscrito.
			 */
			handleSubscription(endpoint);

			/*
			 * Handler para gestionar las desuscripciones de un determinado topic. Haremos
			 * lo contrario que el punto anterior para eliminar al cliente de la lista de
			 * clientes registrados en el topic. De este modo, no seguir� recibiendo
			 * mensajes en este topic.
			 */
			handleUnsubscription(endpoint);

			/*
			 * Este handler ser� llamado cuando se publique un mensaje por parte del cliente
			 * en alg�n topic creado en el servidor MQTT. En esta funci�n obtendremos todos
			 * los clientes suscritos a este topic y reenviaremos el mensaje a cada uno de
			 * ellos. Esta es la tarea principal del broken MQTT. En este caso hemos
			 * implementado un broker muy muy sencillo. Para gestionar QoS, asegurar la
			 * entrega, guardar los mensajes en una BBDD para despu�s entregarlos, guardar
			 * los clientes en caso de ca�da del servidor, etc. debemos recurrir a un c�digo
			 * m�s elaborado o usar una soluci�n existente como por ejemplo Mosquitto.
			 */
			publishHandler(endpoint);

			/*
			 * Handler encargado de gestionar las desconexiones de los clientes al servidor.
			 * En este caso eliminaremos al cliente de todos los topics a los que estuviera
			 * suscrito.
			 */
			handleClientDisconnect(endpoint);
		}).listen(ar -> {
			if (ar.succeeded()) {
				System.out.println("MQTT server est� a la escucha por el puerto " + ar.result().actualPort());
			} else {
				System.out.println("Error desplegando el MQTT server");
				ar.cause().printStackTrace();
			}
		});
	}

	/**
	 * M�todo encargado de gestionar las suscripciones de los clientes a los
	 * diferentes topics. En este m�todo se registrar� el cliente asociado al topic
	 * al que se suscribe
	 * 
	 * @param endpoint
	 */
	private static void handleSubscription(MqttEndpoint endpoint) {
		endpoint.subscribeHandler(subscribe -> {
			// Los niveles de QoS permiten saber el tipo de entrega que se realizar�:
			// - AT_LEAST_ONCE: Se asegura que los mensajes llegan a los clientes, pero no
			// que se haga una �nica vez (pueden llegar duplicados)
			// - EXACTLY_ONCE: Se asegura que los mensajes llegan a los clientes un �nica
			// vez (mecanismo m�s costoso)
			// - AT_MOST_ONCE: No se asegura que el mensaje llegue al cliente, por lo que no
			// es necesario ACK por parte de �ste
			List<MqttQoS> grantedQosLevels = new ArrayList<>();
			for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
				System.out.println("Suscripci�n al topic " + s.topicName() + " con QoS " + s.qualityOfService());
				grantedQosLevels.add(s.qualityOfService());

				// A�adimos al cliente en la lista de clientes suscritos al topic
				clientTopics.put(s.topicName(), endpoint);
			}

			/*
			 * Enviamos el ACK al cliente de que se ha suscrito al topic con los niveles de
			 * QoS indicados
			 */
			endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);
		});
	}

	/**
	 * M�todo encargado de eliminar la suscripci�n de un cliente a un topic. En este
	 * m�todo se eliminar� al cliente de la lista de clientes suscritos a ese topic.
	 * 
	 * @param endpoint
	 */
	private static void handleUnsubscription(MqttEndpoint endpoint) {
		endpoint.unsubscribeHandler(unsubscribe -> {
			for (String t : unsubscribe.topics()) {
				// Eliminos al cliente de la lista de clientes suscritos al topic
				clientTopics.remove(t, endpoint);
				System.out.println("Eliminada la suscripci�n del topic " + t);
			}
			// Informamos al cliente que la desuscripci�n se ha realizado
			endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
		});
	}

	/**
	 * Manejador encargado de notificar y procesar la desconexi�n de los clientes.
	 * 
	 * @param endpoint
	 */
	private static void handleClientDisconnect(MqttEndpoint endpoint) {
		endpoint.disconnectHandler(h -> {
			// Eliminamos al cliente de todos los topics a los que estaba suscritos
			Stream.of(clientTopics.keySet()).filter(e -> clientTopics.containsEntry(e, endpoint))
					.forEach(s -> clientTopics.remove(s, endpoint));
			System.out.println("El cliente remoto se ha desconectado [" + endpoint.clientIdentifier() + "]");
		});
	}

	/**
	 * Manejador encargado de interceptar los env�os de mensajes de los diferentes
	 * clientes. Este m�todo deber� procesar el mensaje, identificar los clientes
	 * suscritos al topic donde se publica dicho mensaje y enviar el mensaje a cada
	 * uno de esos clientes.
	 * 
	 * @param endpoint
	 */
	private static void publishHandler(MqttEndpoint endpoint) {
		endpoint.publishHandler(message -> {
			/*
			 * Suscribimos un handler cuando se solicite una publicaci�n de un mensaje en un
			 * topic
			 */
			handleMessage(message, endpoint);
		}).publishReleaseHandler(messageId -> {
			/*
			 * Suscribimos un handler cuando haya finalizado la publicaci�n del mensaje en
			 * el topic
			 */
			endpoint.publishComplete(messageId);
		});
	}

	/**
	 * M�todo de utilidad para la gesti�n de los mensajes salientes.
	 * 
	 * @param message
	 * @param endpoint
	 */
	private static void handleMessage(MqttPublishMessage message, MqttEndpoint endpoint) {
		/*
		 * System.out.println("Mensaje publicado por el cliente " +
		 * endpoint.clientIdentifier() + " en el topic " + message.topicName());
		 * System.out.println("    Contenido del mensaje: " +
		 * message.payload().toString());
		 */

		/*
		 * Obtenemos todos los clientes suscritos a ese topic (exceptuando el cliente
		 * que env�a el mensaje) para as� poder reenviar el mensaje a cada uno de ellos.
		 * Es aqu� donde nuestro c�digo realiza las funciones de un broken MQTT
		 */
		// System.out.println("Origen: " + endpoint.clientIdentifier());
		List<MqttEndpoint> clientsToRemove = new ArrayList<>();
		for (MqttEndpoint client : clientTopics.get(message.topicName())) {
			// System.out.println("Destino: " + client.clientIdentifier());
			if (!client.clientIdentifier().equals(endpoint.clientIdentifier()))
				try {
					client.publish(message.topicName(), message.payload(), message.qosLevel(), message.isDup(),
							message.isRetain()).publishReleaseHandler(idHandler -> {
								client.publishComplete(idHandler);
							});
				} catch (Exception e) {
					if (e.getMessage().toLowerCase().equals("connection not accepted yet")) {
						clientsToRemove.add(client);
					} else {
						System.out.println("Error, no se pudo enviar mensaje. " + e.getMessage());
					}
				}
		}
		
		if (!clientsToRemove.isEmpty()) {
			clientsToRemove.forEach(clientToRemove -> handleClientDisconnect(clientToRemove));
			clientsToRemove.clear();
		}

		if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
			String topicName = message.topicName();
			switch (topicName) {
			/*
			 * Se podr�a hacer algo con el mensaje como, por ejemplo, almacenar un registro
			 * en la base de datos
			 */
			}
			// Env�a el ACK al cliente de que el mensaje ha sido publicado
			endpoint.publishAcknowledge(message.messageId());
		} else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
			/*
			 * Env�a el ACK al cliente de que el mensaje ha sido publicado y cierra el canal
			 * para este mensaje. As� se evita que los mensajes se publiquen por duplicado
			 * (QoS)
			 */
			endpoint.publishRelease(message.messageId());
		}
	}

}
