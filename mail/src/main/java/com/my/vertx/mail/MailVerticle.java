package com.my.vertx.mail;

import com.my.vertx.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;

/**
 * Created by neil on 2017/9/27.
 */
public class MailVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        MailConfig mailConfig = new MailConfig();
        mailConfig.setHostname("smtp.163.com");
        mailConfig.setSsl(true);
        mailConfig.setPort(465);
        mailConfig.setStarttls(StartTLSOptions.REQUIRED);
        mailConfig.setUsername("xxx@163.com");
        mailConfig.setPassword("xxxxxxx");
        MailClient client = MailClient.createShared(vertx,mailConfig,"xxxx");

        MailMessage message = new MailMessage();
        message.setFrom("xxxx@163.com")
                .setTo("xxxx@qq.com")
                .setCc("xxxxx@kingdee.com")
                .setSubject("vertx mail")
                .setText("Hello From myself");
        client.sendMail(message,result->{
           if (result.failed()) {
               result.cause().printStackTrace();
               System.out.println(result.cause().getLocalizedMessage());
               return;
           }
            String messageId = result.result().getMessageID();
            System.out.println("messageId:" + messageId);
            System.out.println(result.result().toString());
            System.out.println("-------------------------");
            result.result().getRecipients().forEach(System.out::println);
        });
        startFuture.complete();
    }

    public static void main(String[] args) {
        Runner.runExample(MailVerticle.class);
    }
}
