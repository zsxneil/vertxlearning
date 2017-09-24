package com.my.vertx.eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * Created by neil on 2017/9/24.
 */
public class EventBusSenderVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        //send到evenbus的消息只会被一个消费者接受收，publish的会被所有的接收
        vertx.eventBus().send("anAddress","message send")
                        .publish("anAddress","message publish");
    }

}
