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

import org.openremote.controllercommand.ControllerProxyAndCommandServiceApplication;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConnectionHookConsumer implements Runnable {

    private final BlockingQueue<String> queue;

    protected final static org.slf4j.Logger log = LoggerFactory.getLogger(ConnectionHookConsumer.class);
    private final Client client;
    private final String baseUri;
    private final String openPath;
    private final String closePath;
    private final ControllerProxyAndCommandServiceApplication controllerProxyAndCommandServiceApplication;
    private final Map<String,Payload> sessions;
    private final ControllerSessionHandler.ShudownAware isShutdown;

    public ConnectionHookConsumer(BlockingQueue<String> queue, String baseUri, String openPath, String closePath, ControllerProxyAndCommandServiceApplication controllerProxyAndCommandServiceApplication, Map<String,Payload> sessions, ControllerSessionHandler.ShudownAware isShutdown) {
        this.queue = queue;
        this.baseUri = baseUri;
        this.openPath = openPath;
        this.closePath = closePath;
        this.controllerProxyAndCommandServiceApplication = controllerProxyAndCommandServiceApplication;
        this.sessions = sessions;
        this.isShutdown = isShutdown;
        this.client = ClientBuilder.newClient();
    }



    @Override
    public void run() {

        try {
            while (!isShutdown.isShutdownInProgress()) {
                String user = queue.poll(1, TimeUnit.SECONDS);
                if (user != null) {
                    try {
                        String path = sessions.containsKey(user) ? openPath : closePath;
                        Response response = client.target(baseUri)
                                .path(path)
                                .request()
                                .post(Entity.json(sessions.get(user)));
                        log.info("Notify response code : " + response.getStatus());
                        if (response.getStatus() >= 400 && response.getStatus() != 503 ) {
                            //todo log fatal
                        } else if (response.getStatus() != 200) {
                            //todo log info
                            processServerError(user, response.getStatus(), null);
                            queue.put(user);
                          }
                        //todo check response status code
                    } catch (ProcessingException | WebApplicationException ex) {
                        processServerError(user, null, ex);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Closing opening thread", e);
        }
    }

    private void processServerError(String user, Integer status, RuntimeException ex) throws InterruptedException {
    //todo log
        queue.put(user);
        Thread.sleep(3000); // todo params
    }


}
