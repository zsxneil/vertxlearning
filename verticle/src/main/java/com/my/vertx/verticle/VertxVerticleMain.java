package com.my.vertx.verticle;

import io.vertx.core.Vertx;

/**
 * Created by neil on 2017/9/24.
 */
public class VertxVerticleMain {
    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new BasicVerticle(),result->{
            System.out.println(result.result());
            vertx.undeploy(result.result(),r->{
                System.out.println("undeployment success");
                vertx.close();
            });
        });
    }
}
