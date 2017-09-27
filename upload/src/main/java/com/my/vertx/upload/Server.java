package com.my.vertx.upload;

import com.my.vertx.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Created by neil on 2017/9/27.
 */
public class Server extends AbstractVerticle {

    public static void main(String[] args) {
        Runner.runExample(Server.class);
    }

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        String tmpdir = System.getProperty("java.io.tmpdir");
        System.out.println(tmpdir);
        // Enable multipart form data parsing
        router.route().handler(BodyHandler.create().setUploadsDirectory(tmpdir));

        router.route("/").handler(ctx->{
          ctx.response().putHeader("content-type","text/html;charset=utf-8").end(
                  "<form action=\"/form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                          "    <div>\n" +
                          "        <label for=\"name\">Select a file:</label>\n" +
                          "        <input type=\"file\" name=\"file\" />\n" +
                          "    </div>\n" +
                          "    <div class=\"button\">\n" +
                          "        <button type=\"submit\">Send</button>\n" +
                          "    </div>" +
                          "</form>"
          );
        });

        router.post("/form").handler(ctx->{
           ctx.response().putHeader("content-type","text/plain;charset=utf-8")
                            .setChunked(true);
           for (FileUpload fileUpload : ctx.fileUploads()) {
               System.out.println("f");
               ctx.response().write("filename:" + fileUpload.fileName())
                       .write("\n")
                       .write("size:" + fileUpload.size())
                       .write("\n")
                        .write(fileUpload.uploadedFileName());

           }
           ctx.response().end();
        });

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }
}
