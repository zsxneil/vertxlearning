package com.my.vertx.eventbus;

import io.vertx.core.Vertx;

/**
 * Created by neil on 2017/9/24.
 */
public class EventBusMain {
    public static void main(String[] args) throws InterruptedException {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new EventBusReceiverVerticle("R1"));
        vertx.deployVerticle(new EventBusReceiverVerticle("R2"));

        Thread.sleep(3000);
        vertx.deployVerticle(new EventBusSenderVerticle());
    }
}
