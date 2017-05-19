/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2016, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.controllercommand.resources;

import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.concurrent.BlockingQueue;

public class ConnectionHookConsumer implements Runnable{

    private final BlockingQueue<Payload> queue;

    protected final static org.slf4j.Logger log = LoggerFactory.getLogger(ConnectionHookConsumer.class);
    private final Client client;
    private final String baseUri;
    private final String path;


    public ConnectionHookConsumer(BlockingQueue<Payload> queue, String baseUri, String path){
        this.queue = queue;
        this.baseUri = baseUri;
        this.path = path;
        this.client = ClientBuilder.newClient();
    }

    @Override
    public void run() {

        try{
            while(true){
                Payload payload = queue.take();
                try {
                    Response response = client.target(baseUri)
                            .path(path)
                            .request()
                            .post(Entity.json(payload));
                    log.info("Notify response code : "+response.getStatus());
                } catch  (ProcessingException | WebApplicationException ex ) {
                    log.error("Error processing WS HOOK", ex);
                    queue.put(payload);
                }
                Thread.sleep(100);
            }
        } catch(InterruptedException e) {
            log.error("Closing opening thread",e);
        }
    }


}
