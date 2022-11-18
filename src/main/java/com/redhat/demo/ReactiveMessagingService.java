package com.redhat.demo;

import java.time.Duration;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.annotations.Merge;

@ApplicationScoped
public class ReactiveMessagingService {
    private static final Logger log = Logger.getLogger(ReactiveMessagingService.class);

    /**
     * A message sink
     * Message will be processed one by one in a syc or async way
     * It should return a typical void type, or Uni<Void>, CompletionStage<Void>
     * Ether the completion of method call or Uni/CompletionStage will acknowledge
     * the message automatically.
     * 
     * Uni<T> represents an asyn item
     */
    @Incoming("sink")
    public void concur(String msg) {
        log.info("Concur incoming message: [" + msg + "]");
    }

    /**
     * Another message sink
     * Smallrye reactive messaging supports a 'fan-out' pattern by
     * allowing multiple consumers connecting to the same
     * channel given that the producer method must be annotated
     * with @Broadcast
     * 
     * @param msg
     */
    @Incoming("sink")
    // Use vert.x worker pool thread
    @Blocking()
    public void reckon(String msg) {
        log.info("Received incoming message: [" + msg + "]");
    }

    /**
     * A channel is just a named instance of a Connector
     * The Incoming channel is a consumer connector instance
     * It depends on the implementation and configuration
     * of the Connector for actual behavior. For example,
     * it could be a kafka consumer, or a http server side listening 
     * port. 
     */
    @Incoming("hello")
    public void hello(String msg) {
        log.info("Hello " + msg + "!!!");
    }

    /**
     * Multi<T> represents a reactive/asyn stream
     * Only processor can take input and return reactive stream
     * (aka Multi<T>)
     */
    // Incoming-Consumer-Merge (ICM) - Multi<T>
    @Incoming("source")
    // BUG: The default behavior of merging sync producers
    // looks like Merge.ONE instead of Merge.MERGE
    // Merge == Merge messages produced by multiple producers from the same channel
    @Merge

    // Alternatively, it merges message from different channels
    @Incoming("delayed")

    // Outgoing-Producer-Broadcast (OPB) - Emitter<T>
    @Outgoing("sink")
    // IMPORTANT: Must have an entry barrier to wait for all consumers getting
    // ready before dispatching messages
    @Broadcast(2)
    public Multi<String> processor(Multi<String> messages) {
        return messages.select().first(20) // take one the first few messages
                .onItem().transform(m -> m.toUpperCase());
    }

    /**
     * A message source
     */
    @Outgoing("source")
    public Multi<String> generator() {
        // Generates an async/reactive stream with limited items (2)
        return Multi.createFrom().items("Hey", "Quarkus");
    }

    /**
     * Another messsage source
     */
    // Smallrye supports a 'join' (aka Merge) pattern which allows
    // multiple producers, annotated with Outgoing channel
    // to produce messages which are consumed by one consumer
    // method, annotated with a same name of an Incoming channel
    @Outgoing("source")
    public Multi<String> producer() {
        return Multi.createFrom().items("Reactive", "cool");
    }

    /**
     * A stream of randomly delayed Uni, which is to simulate
     * service callout with various response time, but still
     * maintains the original order of the requests sent
     * @return
     */
    @Outgoing("delayed")
    public Multi<String> randomTimedPublisher() {   
        Random r = new Random();
        return Multi.createFrom()
            .range(0, 10)
            // .onItem() 
            //     // Event observing methods, invoke (sync) and call (async) 
            //     .invoke(i -> { throw new UnsupportedOperationException(
            //         """
            //             Even if the execption is raised at observeing method,
            //             it needs to be capture and handled; otherwise will blowup
            //             the entire stream/pipeline
            //         """);})
            // .onItem()
            //     .call(i -> Uni.createFrom().failure(new UnsupportedOperationException(
            //         """
            //             Pipleline flow will be blocked by the async obersering method.
            //             The flow resumes once the Uni emmit an item, which could be
            //             an exception.
            //         """)))
            .onItem()
                // Unit-of-Order processing
                // .transformToUniAndMerge(
                .transformToUniAndConcatenate(
                    i -> Uni.createFrom().item(i.toString().concat("- delayed"))
                            .onItem().delayIt().by(Duration.ofMillis(r.nextInt(100))))
            // Pipeline level exception handler
            // By default, the failure handling will terminate current pipeline/stream processing
            // a pattern could be: retry Uni and replaying Multi
            .onFailure().recoverWithItem("Recovered from failure");
    }
}
