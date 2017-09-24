package com.my.vertx.eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * Created by neil on 2017/9/24.
 */
public class EventBusReceiverVerticle extends AbstractVerticle{
    private String name;

    public EventBusReceiverVerticle(String name) {
        this.name = name;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        vertx.eventBus().consumer("anAddress",message->{
            System.out.println(this.name + " received message: " + message.body());
        });
    }
}
