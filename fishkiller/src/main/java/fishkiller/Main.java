package fishkiller;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * Hello world!
 *
 */
public class Main extends AbstractVerticle {
	public void start(Future<Void> startFuture) {
  
		vertx.deployVerticle(new Pecera());
		vertx.deployVerticle(new VertxMqttServer());
    }
}
