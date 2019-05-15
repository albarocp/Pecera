package fishkiller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class Pecera extends AbstractVerticle {
	
	private AsyncSQLClient bdPecera;
	
	public void start(Future<Void> StartFuture) {
		
		JsonObject config = new JsonObject()
				.put("host", "localhost")
				.put("username", "root")
				.put("password", "root")
				.put("database", "fishkiller")
				.put("port", 3306);
		
		bdPecera = MySQLClient.createShared(vertx, config);
		Router router = Router.router(vertx);
		
		vertx.createHttpServer().requestHandler(router)
			 .listen(8080, estado -> {
					if (estado.succeeded()) {
						
						System.out.println("Vertx desplegado");
					}else {
						
						System.out.println("Error al desplegar Vertx");
					}
				 					 }
					);
		
		router.route("/").handler(rc -> {
            HttpServerResponse response = rc.response();
            response
                .putHeader("content-type", "text/html")
                .end("<h1>Hello from my first Vert.x 3 application</h1>");
        });
		
		router.route().handler(BodyHandler.create());
		router.get("/info/:nametable").handler(this::getInformacion);
		
		router.put("/add/:nametable").handler(this::addMedicion);
		router.put("/addinfo/:nametable").handler(this::addUsuarioODispositivo);
		
		router.put("/updateinfo").handler(this::updateUsuarioODispositivo);
		
		router.delete("/delete").handler(this::deleteInfo);
		
	}
	
	private void getInformacion(RoutingContext rc) {
		
		String nameTable = rc.request().getParam("nametable");
		String sql = "SELECT * FROM "+nameTable;
		
		bdPecera.getConnection(connection ->{
			if(connection.succeeded()) {
				
				connection.result().query(sql, result -> {
					if(result.succeeded()) {
						
						String jsonResult = result.result().toJson().encodePrettily();
						rc.response().end(jsonResult);
					}else {
						
						System.out.println(result.cause().getMessage());
						rc.response().setStatusCode(400).end();
						
					}
					
					connection.result().close();
				});
			}else {
				
				
				System.out.println(connection.cause().getMessage());
				rc.response().setStatusCode(400).end();	
				connection.result().close();
			}	
		});

	}
	
	private void addMedicion(RoutingContext rc) {
		String nameTable = rc.request().getParam("nametable");
	
		String sql = "INSERT INTO "+nameTable+ "(nivel, fkiddispositivo) VALUES (?, ?)";
		JsonObject datos = rc.getBodyAsJson();
		
		bdPecera.getConnection(connection ->{
			
			if(connection.succeeded()) {
				connection.result().updateWithParams(sql, new JsonArray().add(datos.getFloat("nivel")).add(datos.getInteger("fkiddispositivo")), res ->{
					 if (res.failed()) {
						 rc.response().setStatusCode(400).end();	
						   } else {
							   rc.response().setStatusCode(200).end();	
								connection.result().close();
						   }
				});
				connection.result().close();
				
			}else {
				
				System.out.println(connection.cause().getMessage());
				rc.response().setStatusCode(400).end();	
				connection.result().close();
			}	
		});
					
	}
	
	private void addUsuarioODispositivo(RoutingContext rc) {
		String nameTable = rc.request().getParam("nametable");
	

		
		JsonObject datos = rc.getBodyAsJson();
		
		if(datos.getInteger("idusuario")==null) {
			String sql = "INSERT INTO "+nameTable+ "(nombre) VALUES (?)";
			bdPecera.getConnection(connection ->{
				
				if(connection.succeeded()) {
					connection.result().updateWithParams(sql, new JsonArray().add(datos.getString("nombre")), res ->{
						 if (res.failed()) {
							 rc.response().setStatusCode(400).end();	
							   } else {
								   rc.response().setStatusCode(200).end();	
									connection.result().close();
							   }
					});
					connection.result().close();
					
				}else {
					
					System.out.println(connection.cause().getMessage());
					rc.response().setStatusCode(400).end();	
					connection.result().close();
				}	
			});
			
			
		}else {
		String sql = "INSERT INTO "+nameTable+ "(fkidusuario, nombre) VALUES (?, ?)";
		bdPecera.getConnection(connection ->{
			
			if(connection.succeeded()) {
				connection.result().updateWithParams(sql, new JsonArray().add(datos.getInteger("idusuario")).add(datos.getString("nombre")), res ->{
					 if (res.failed()) {
						 rc.response().setStatusCode(400).end();	
						   } else {
							   rc.response().setStatusCode(200).end();	
								connection.result().close();
						   }
				});
				connection.result().close();
				
			}else {
				
				System.out.println(connection.cause().getMessage());
				rc.response().setStatusCode(400).end();	
				connection.result().close();
			}	
		});
		}
					
	}
	
	private void updateUsuarioODispositivo(RoutingContext rc) {
		JsonObject datos = rc.getBodyAsJson();
		
		if(datos.getInteger("iddispositivo")!=null) {
			String sql = "UPDATE "+datos.getString("nametable")+" SET nombre = ? WHERE iddispositivo = ?";
			bdPecera.getConnection(connection ->{
				
				if(connection.succeeded()) {
					connection.result().updateWithParams(sql, new JsonArray().add(datos.getString("nuevodispositvo")).add(datos.getInteger("iddispositivo")), res ->{
						 if (res.failed()) {
							 rc.response().setStatusCode(400).end();	
							   } else {
								   rc.response().setStatusCode(200).end();	
									connection.result().close();
							   }
					});
					connection.result().close();
					
				}else {
					
					System.out.println(connection.cause().getMessage());
					rc.response().setStatusCode(400).end();	
					connection.result().close();
				}	
			});
			
		}else {
		String sql = "UPDATE "+datos.getString("nametable")+" SET nombre = ? WHERE idusuario = ?";
		bdPecera.getConnection(connection ->{
			
			if(connection.succeeded()) {
				connection.result().updateWithParams(sql, new JsonArray().add(datos.getString("nuevous")).add(datos.getInteger("idus")), res ->{
					 if (res.failed()) {
						 rc.response().setStatusCode(400).end();	
						   } else {
							   rc.response().setStatusCode(200).end();	
								connection.result().close();
						   }
				});
				connection.result().close();
				
			}else {
				
				System.out.println(connection.cause().getMessage());
				rc.response().setStatusCode(400).end();	
				connection.result().close();
			}	
		});
		}
		
	}
	
	private void deleteInfo(RoutingContext rc) {
		JsonObject datos = rc.getBodyAsJson();
		String nametable = datos.getString("nametable");
		Integer id;
		String sql;
		
		if(datos.getInteger("iddispositivo")!=null) {
			
			sql = "DELETE FROM "+nametable+" WHERE iddispositivo = ?";
			id = datos.getInteger("iddispositivo");
		}else {
			
			sql = "DELETE FROM "+nametable+" WHERE idusuario = ?";
			id = datos.getInteger("idusuario");
		}
		
		bdPecera.getConnection(connection ->{
			
			if(connection.succeeded()) {
				connection.result().updateWithParams(sql, new JsonArray().add(id), res ->{
					 if (res.failed()) {
						 rc.response().setStatusCode(400).end();	
						   } else {
							   rc.response().setStatusCode(200).end();	
								connection.result().close();
						   }
				});
				connection.result().close();
				
			}else {
				
				System.out.println(connection.cause().getMessage());
				rc.response().setStatusCode(400).end();	
				connection.result().close();
			}	
		});
		
	}

}
