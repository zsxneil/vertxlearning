package com.my.vertx.verticle;

import io.vertx.core.AbstractVerticle;

/**
 * Created by neil on 2017/9/24.
 */
public class BasicVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        System.out.println("Basic Verticle started");
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Basic Verticle stoped");
    }
}
