package com.redhat.demo;

import java.time.LocalTime;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.annotations.Broadcast;

@Path("/")
public class GreetingResource {
    private static final Logger log = Logger.getLogger(GreetingResource.class);
    
    @Inject
    @Channel("hello")       // An outgoing channel
    @Broadcast()            // To support multiple downstreams
    MutinyEmitter<String> emitter;

    @Inject
    @Channel("hello-from-kafka")    // An incoming channel
    Multi<String> tapper;           // An imperative consumer

    @GET
    @Path("hello")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> hello() {
        // Simply return the reactive stream which has already
        // been hooked up with a consumer connector for kafka.
        // Quarkus resteasy reactive library will do the rest.
        return tapper;              
    }

    @GET
    @Path("say/{something}")
    @Produces(MediaType.TEXT_PLAIN)
    public String say(String something) {
        String prefix = "Message [" + LocalTime.now() + "] : ";
        if (emitter.hasRequests()) {
            // Since MutinyEmitter is asynch by nature, 
            // don't use like this:
            // emitter.send(name); 
            emitter.sendAndAwait(prefix + something);
            log.debug("A message [" + something + "] send to kafka topic");
        } else {
            log.warn("downstream consumer not ready!");
        }

        return  prefix + something;
    }
}