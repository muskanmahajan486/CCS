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

import javax.websocket.Session;
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
    private final Map<String, Payload> connectedControllerByUser;
    private final Map<String, Session> sessions;
    private final ControllerSessionHandler.ShudownAware isShutdown;
    private final long retryTimeout;

    public ConnectionHookConsumer(BlockingQueue<String> queue, String baseUri, String openPath, String closePath, ControllerProxyAndCommandServiceApplication controllerProxyAndCommandServiceApplication, Map<String, Payload> connectedControllerByUser, Map<String, Session> sessions, ControllerSessionHandler.ShudownAware isShutdown, long retryTimeout) {
        this.queue = queue;
        this.baseUri = baseUri;
        this.openPath = openPath;
        this.closePath = closePath;
        this.controllerProxyAndCommandServiceApplication = controllerProxyAndCommandServiceApplication;
        this.connectedControllerByUser = connectedControllerByUser;
        this.sessions = sessions;
        this.isShutdown = isShutdown;
        this.retryTimeout = retryTimeout;
        this.client = ClientBuilder.newClient();
    }


    @Override
    public void run() {

        try {
            while (!isShutdown.isShutdownInProgress()) {
                String user = queue.poll(1, TimeUnit.SECONDS);
                if (user != null && connectedControllerByUser.containsKey(user)) {
                    try {
                        String path = sessions.containsKey(user) ? openPath : closePath;

                        Response response = client.target(baseUri)
                                .path(path)
                                .request()
                                .post(Entity.json(connectedControllerByUser.get(user)));
                        int status = response.getStatus();
                        if (status >= 400 && status != 503) {
                            log.error("WS notification get fatal response code :" + status);
                        } else if (status == 200 && path.equals(closePath)) {
                            connectedControllerByUser.remove(user);
                        } else {
                            processServerError(user, status, null);
                        }

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
        if (status != null) {
            log.info("WS notification get wrong response code : " + status + " retry in " + retryTimeout + " ms");
        }
        if (ex != null) {
            log.info("WS notification get exception:", ex);
        }
        queue.put(user);
        Thread.sleep(retryTimeout);
    }


}
