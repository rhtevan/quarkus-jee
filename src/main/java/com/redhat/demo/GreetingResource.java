package com.redhat.demo;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import io.smallrye.reactive.messaging.annotations.Broadcast;

@Path("/hello")
public class GreetingResource {
    @Inject
    @Channel("hello")
    @Broadcast()
    Emitter<String> emitter;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        emitter.send("from the other side");
        return "Hello from RESTEasy Reactive";
    }

    @GET
    @Path("{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello(String name) {
        emitter.send(name);
        return "Send message to " + name;
    }
}