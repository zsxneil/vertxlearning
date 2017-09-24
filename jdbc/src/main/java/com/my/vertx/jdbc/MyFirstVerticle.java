package com.my.vertx.jdbc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * after build and package with maven ,
 * you can start up the app use 'java -jar target/my-first-app-1.0-SNAPSHOT-fat.jar -conf src/main/conf/my-application-conf.json'
 * Created by neil on 2017/9/21.
 */
public class MyFirstVerticle extends AbstractVerticle {

    private JDBCClient jdbc;


    private void getAll(RoutingContext context) {
        jdbc.getConnection(ar->{
            SQLConnection connection = ar.result();
            connection.query("SELECT * FROM Whisky",(result)->{
                List<Whisky> whiskyList = result.result().getRows().stream().map(Whisky::new).collect(Collectors.toList());
                context.response().
                        putHeader("content-type","application/json;charset=utf-8")
                        .end(Json.encodePrettily(whiskyList));
                connection.close();
            });
        });
    }

    private void addOne(RoutingContext context) {
        jdbc.getConnection(ar->{
            SQLConnection connection = ar.result();
            final Whisky whisky = Json.decodeValue(context.getBodyAsString(),Whisky.class);
            insert(whisky,connection,(result)->{
                Whisky w = result.result();
                context.response()
                        .setStatusCode(201)
                        .putHeader("content-type","application/json;charset=utf-8")
                        .end(Json.encodePrettily(w));
                connection.close();
            });
        });
    }

    private void deleteOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
        } else {
            jdbc.getConnection((ar)->{
               SQLConnection connection = ar.result();
               connection.execute("delete from Whisky where id = '" + id + "'",
                       (result)->{
                            context.response().setStatusCode(204).end();
                            connection.close();
                        });
            });
        }
    }

    private void updateOne(RoutingContext context) {
        String id = context.request().getParam("id");
        JsonObject json = context.getBodyAsJson();
        if (id == null || json == null) {
            context.response().setStatusCode(400).end();
        } else {
            jdbc.getConnection((ar)->{
                SQLConnection connection = ar.result();
                update(id,json,connection,(result)->{
                    if (result.failed()) {
                        context.response().setStatusCode(404).end();
                    } else {
                        context.response()
                                .putHeader("content-type","application/json;charset=utf-8")
                                .end(Json.encodePrettily(result.result()));
                    }
                });
                connection.close();
            });
        }
    }

    private void findOne(RoutingContext context) {
        String id = context.request().getParam("id");
        JsonObject json = context.getBodyAsJson();
        if (id == null || json == null) {
            context.response().setStatusCode(400).end();
        } else {
            jdbc.getConnection((ar)->{
                SQLConnection connection = ar.result();
                select(id,connection,(result)->{
                    if (result.failed()) {
                        context.response().setStatusCode(404).end();
                    } else {
                        context.response()
                                .putHeader("content-type","application/json;charset=utf-8")
                                .end(Json.encodePrettily(result.result()));
                    }
                });
                connection.close();
            });
        }
    }


    /**
     * This method is called when the verticle is deployed. It creates a HTTP server and registers a simple request
     * handler.
     * <p/>
     * Notice the `listen` method. It passes a lambda checking the port binding result. When the HTTP server has been
     * bound on the port, it call the `complete` method to inform that the starting has completed. Else it reports the
     * error.
     *
     * @param fut the future
     */
    @Override
    public void start(Future<Void> fut) throws Exception {

        jdbc = JDBCClient.createShared(vertx,config(),"My-Whisky-Collection");

        startBackend(
                (connection)->createSomeData(
                        connection,
                        (nothing)->startWebApp(
                                (http)->completeStartup(http,fut)
                        )
                        ,fut)
                ,fut);
    }

    private void startBackend(Handler<AsyncResult<SQLConnection>> next,Future<Void> fut){
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                fut.fail(ar.cause());
            } else {
                next.handle(Future.succeededFuture(ar.result()));
            }
        });
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        //create a Router Object
        Router router = Router.router(vertx);

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(routerContext -> {
            HttpServerResponse response = routerContext.response();
            response.putHeader("content-type","text/html")
                    .end("<h1>hello from my first vert.x 3 application</h1>");
        });

        router.get("/api/whiskies").handler(this::getAll);
        router.route("/api/whiskies*").handler(BodyHandler.create());
        router.post("/api/whiskies").handler(this::addOne);
        router.delete("/api/whiskies/:id").handler(this::deleteOne);
        router.put("/api/whiskies/:id").handler(this::updateOne);
        router.get("/api/whiskies/:id").handler(this::findOne);
        router.route("/assets/*").handler(StaticHandler.create("assets"));

        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config().getInteger("http.port",8080),next::handle);

    }

    private void completeStartup(AsyncResult<HttpServer> http,Future<Void> fut) {
        if (http.succeeded()) {
            fut.complete();
        } else {
            fut.fail(http.cause());
        }
    }

    @Override
    public void stop() throws Exception {
        // Close the JDBC client.
        jdbc.close();
    }

    private void createSomeData(AsyncResult<SQLConnection> result,Handler<AsyncResult<Void>> next,Future<Void> fut) {
       if (result.failed()) {
           fut.fail(result.cause());
       } else {
           SQLConnection connection = result.result();
           connection.execute("CREATE TABLE IF NOT EXISTS Whisky(id Integer IDENTITY,name varchar(100),origin varchar(100))",
                   ar->{
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                            connection.close();
                            return;
                        }
                        connection.query("SELECT * FROM Whisky",select->{
                            if (select.failed()) {
                                fut.fail(select.cause());
                                connection.close();
                                return;
                            }
                            if (select.result().getNumRows() == 0) {
                                insert(new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay"),connection,
                                        (v)->insert(new Whisky("Talisker 57° North", "Scotland, Island"),connection,
                                                (r)->{
                                                    next.handle(Future.succeededFuture());
                                                    connection.close();
                                                })
                                        );
                            } else {
                                next.handle(Future.succeededFuture());
                                connection.close();
                            }
                        });
                   });

       }
    }

    public void insert(Whisky whisky, SQLConnection connection, Handler<AsyncResult<Whisky>> next) {
        String sql = "INSERT INTO Whisky (name,origin) VALUES ?,?";
        connection.updateWithParams(sql,new JsonArray().add(whisky.getName()).add(whisky.getOrigin()),(ar)->{
            if (ar.failed()) {
                next.handle(Future.failedFuture(ar.cause()));
                connection.close();
                return;
            }
            UpdateResult result = ar.result();
            Whisky w = new Whisky(result.getKeys().getInteger(0),whisky.getName(),whisky.getOrigin());
            next.handle(Future.succeededFuture(w));
        });
    }

    public void update(String id,JsonObject json,SQLConnection connection,Handler<AsyncResult<Whisky>> next) {
        String sql = "UPDATE Whisky set name=?,origin=? WHERE id=?";
        connection.updateWithParams(sql,
                new JsonArray()
                        .add(json.getString("name"))
                        .add(json.getString("origin"))
                        .add(id),
                (update)->{
                    if (update.failed()) {
                        next.handle(Future.failedFuture("can't update the Whisky"));
                        return;
                    }
                    if (update.result().getUpdated() == 0) {
                        next.handle(Future.failedFuture("Whisky not found"));
                        return;
                    }
                    next.handle(Future.succeededFuture(
                            new Whisky(Integer.valueOf(id),json.getString("name"),json.getString("origin")))
                    );
                });
    }

    public void select(String id,SQLConnection connection,Handler<AsyncResult<Whisky>> resultHandler) {
        String sql = "SELECT * FROM Whisky WHERE id=?";
        connection.queryWithParams(sql,new JsonArray().add(id),ar->{
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture("Whisky not found"));
            } else {
                if (ar.result().getNumRows() >= 1) {
                    resultHandler.handle(Future.succeededFuture(new Whisky(ar.result().getRows().get(0))));
                } else {
                    resultHandler.handle(Future.failedFuture("Whisky not found"));
                }
            }
        });
    }
}
