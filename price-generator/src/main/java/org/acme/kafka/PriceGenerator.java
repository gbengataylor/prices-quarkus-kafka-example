package org.acme.kafka;

import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.Flowable;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A bean producing random prices every 5 seconds.
 * The prices are written to a Kafka topic (prices). The Kafka configuration is specified in the application configuration.
 */
@ApplicationScoped
public class PriceGenerator {

    private Random random = new Random();

    @Outgoing("generated-price")                        
    public Flowable<Integer> generate() {      
        Flowable<Integer> number =  Flowable.interval(5, TimeUnit.SECONDS)
                .map(tick -> random.nextInt(100));
        System.out.println("generated number " + number.toString());
        return number;
    } 

    @ConsumeEvent("prices")
    //@Outgoing("generated-price")
    public Integer generatePrice(Integer price) {      
       
        System.out.println("generated price " + price);
        return price;
    }        

}