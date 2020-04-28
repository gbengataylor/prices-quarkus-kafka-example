package org.acme.kafka;

import java.util.Random;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.axle.core.eventbus.EventBus;

/**
 * A simple resource retrieving the in-memory "my-data-stream" and sending the items as server-sent events.
 */
@Path("/gen-price")
public class PriceGeneratorResource {


      @Inject EventBus bus;
          private Random random = new Random();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Integer generatePrice() { 
         int price = random.nextInt(100);
         bus.<Integer>send("prices", price);
        return price;
    }
}