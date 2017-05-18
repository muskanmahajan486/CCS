package org.openremote.controllercommand.resources;

import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.concurrent.BlockingQueue;

/**
 * Created by mica on 17/05/17.
 */
public class ConnectionHookConsumer implements Runnable{

    private final BlockingQueue<String> queue;

    protected final static org.slf4j.Logger log = LoggerFactory.getLogger(ConnectionHookConsumer.class);
    private final Client client;
    private final String baseUri;
    private final String path;


    public ConnectionHookConsumer(BlockingQueue<String> queue, String baseUri, String path){
        this.queue = queue;
        this.baseUri = baseUri;
        this.path = path;
        this.client = ClientBuilder.newClient();
    }

    @Override
    public void run() {

        try{
            while(true){
                String msg = queue.take();
                try {
                    Response response = client.target(baseUri)
                            .path(path)
                            .request()
                            .post(Entity.json(msg));
                    log.info("Notify response code : "+response.getStatus());
                } catch  (ProcessingException | WebApplicationException ex ) {
                    log.error("Error processing WS HOOK", ex);
                    queue.put(msg);
                }
                Thread.sleep(100);
            }
        } catch(InterruptedException e) {
            log.error("Closing opening thread",e);
        }
    }
}
