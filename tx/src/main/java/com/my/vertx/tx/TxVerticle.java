package com.my.vertx.tx;

import com.my.vertx.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by neil on 2017/9/25.
 */
public class TxVerticle extends AbstractVerticle {

    private static final Logger logger = LogManager.getLogger(TxVerticle.class);

    private JDBCClient jdbcClient;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        jdbcClient = JDBCClient.createShared(vertx,new JsonObject()
                                .put("url","jdbc:mysql://127.0.0.1:3306/vertx?characterEncoding=utf8&useSSL=false")
                                .put("driver_class","com.mysql.jdbc.Driver")
                                .put("user","root")
                                .put("password","123456")
                                .put("max_pool_size",30));
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route("/hello").handler(r->{
           r.response().end("hello");
        });

        router.get("/db").handler(this::queryDB);
        router.get("/tx").handler(this::tx);
        router.get("/tx2").handler(this::tx2);

        vertx.createHttpServer().requestHandler(router::accept).listen(8080,"localhost",r->{
            if (r.succeeded()) {
                logger.info("服务启动成功，端口8080");
                System.out.println("服务启动成功，端口8080");
            } else {
                logger.error("服务启动失败，err:" + r.cause().getLocalizedMessage());
                System.err.println("服务启动失败，err:" + r.cause().getLocalizedMessage());
            }
        });
    }

    private void tx2(RoutingContext context) {
        final String s1 = "INSERT INTO movie (id, name, duration, director) VALUES ('1432414', 'xxx124', 20, '124xxx')";
        final String s2 = "INSERT INTO movie (id, name, duration, director) VALUES ('1432415', 'xxx125', 21, '125xxx')";
        final String s3 = "INSERT INTO movie (id, name_, duration, director) VALUES ('1432416', 'xxx126', 22, '126xxx')";

        jdbcClient.getConnection(conn->{
            if (conn.succeeded()) {
                final SQLConnection connection = conn.result();
                List<Future> futureList = new ArrayList<>();
                Future<Void> future = Future.future();
                connection.setAutoCommit(false,future); //手动开启事务
                futureList.add(future);

                Future<UpdateResult> future1 = Future.future();
                connection.update(s1,future1);
                futureList.add(future1);

                Future<UpdateResult> future2 = Future.future();
                connection.update(s1,future2);
                futureList.add(future2);

                Future<UpdateResult> future3 = Future.future();
                connection.update(s1,future3);
                futureList.add(future3);

                CompositeFuture.all(futureList).setHandler(res->{
                    if (res.failed()) {
                        logger.error("事务失败:" + res.cause().getLocalizedMessage());
                        System.err.println("事务失败:" + res.cause().getLocalizedMessage());
                        connection.rollback(roll->{
                            if (roll.failed()) {
                                logger.error("回滚失败:" + roll.cause().getLocalizedMessage());
                                System.err.println("回滚失败:" + roll.cause().getLocalizedMessage());
                                context.response().setStatusCode(500).end("rollback error");
                            } else {
                                logger.info("回滚成功");
                                System.out.println("回滚成功");
                                context.response().setStatusCode(500).end("rollback ok");
                            }
                            connection.close();
                        });
                    } else {
                        logger.info("事务成功");
                        System.out.println("事务成功");
                        connection.commit(com->{
                            if (com.succeeded()) {
                                logger.info("事务提交成功");
                                System.out.println("事务提交成功");
                                context.response().setStatusCode(200).end("ok");
                            } else {
                                logger.error("事务提交失败：" + com.cause().getLocalizedMessage());
                                System.err.println("事务提交失败：" + com.cause().getLocalizedMessage());
                                context.response().setStatusCode(500).end("commit error");
                            }
                            connection.close();
                        });
                    }
                });
            }
        });
    }

    // 下面的代码类似
    // try{
    //  // 开启事务
    //  ...
    // // 提交事务
    // } catch (Exception e) {
    // // 回滚事务
    // }
    private void tx(RoutingContext context) {
        jdbcClient.getConnection(conn->{
            final SQLConnection connection = conn.result();
            String s1 = "INSERT INTO movie (id, name, duration, director) VALUES ('1432414', 'xxx124', 20, '124xxx')";
            String s2 = "INSERT INTO movie (id, name, duration, director) VALUES ('1432415', 'xxx125', 21, '125xxx')";
            String s3 = "INSERT INTO movie (id, name, duration, director) VALUES ('1432416', 'xxx126', 22, '126xxx')";
            Future.<Void>future(f->{
                connection.setAutoCommit(false,f);
            }).compose(f1->
                Future.<UpdateResult>future(f->
                    connection.update(s1,f)
                )
            ).compose(f2->
                Future.<UpdateResult>future(f3->
                    connection.update(s2,f3)
                )
            ).compose(f4->
                Future.<UpdateResult>future(f5->
                    connection.update(s3,f5)
                )
            ).setHandler(res->{
                if (res.failed()) {
                    logger.error("事务失败:" + res.cause().getLocalizedMessage());
                    System.err.println("事务失败:" + res.cause().getLocalizedMessage());
                    connection.rollback(roll->{
                        if (roll.failed()) {
                            logger.error("回滚失败:" + roll.cause().getLocalizedMessage());
                            System.err.println("回滚失败:" + roll.cause().getLocalizedMessage());
                            context.response().setStatusCode(500).end("rollback error");
                            connection.close();
                        } else {
                            logger.info("回滚成功");
                            System.out.println("回滚成功");
                            context.response().setStatusCode(500).end("rollback ok");
                            connection.close();
                        }
                    });
                } else {
                    logger.info("事务成功");
                    System.out.println("事务成功");
                    connection.commit(com->{
                       if (com.succeeded()) {
                           logger.info("事务提交成功");
                           System.out.println("事务提交成功");
                           context.response().setStatusCode(200).end("ok");
                           connection.close();
                       } else {
                           logger.error("事务提交失败：" + com.cause().getLocalizedMessage());
                           System.err.println("事务提交失败：" + com.cause().getLocalizedMessage());
                           context.response().setStatusCode(500).end("commit error");
                           connection.close();
                       }
                    });
                }
            });
        });
    }

    private void queryDB(RoutingContext context) {
        jdbcClient.getConnection(conn->{
            if (conn.failed()) {
                System.out.println(conn.cause().getLocalizedMessage());
                context.response().setStatusCode(500).end();
            } else {
                final SQLConnection connection = conn.result();
                Future<ResultSet> future = Future.future();
                future.setHandler(res->{
                    if (res.succeeded()) {
                        List<JsonObject> list = res.result().getRows();
                        if (list !=  null && list.size() > 0) {
                            context.response().end(Json.encodePrettily(list));
                            connection.close();
                        }
                    } else {
                        System.err.println("查询出错：" + res.cause().getLocalizedMessage());
                        connection.close();
                    }
                });

                final String sql = "select * from movie";
                connection.queryWithParams(sql,null, future);
            }
        });
    }

    public static void main(String[] args) {
        Runner.runExample(TxVerticle.class);
    }
}
