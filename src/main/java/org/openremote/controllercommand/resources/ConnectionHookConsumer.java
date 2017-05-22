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
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.service.AccountService;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.websocket.Session;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ConnectionHookConsumer implements Runnable {

    private final BlockingQueue<String> queue;

    protected final static org.slf4j.Logger log = LoggerFactory.getLogger(ConnectionHookConsumer.class);
    private final Client client;
    private final String baseUri;
    private final String openPath;
    private final String closePath;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ControllerProxyAndCommandServiceApplication controllerProxyAndCommandServiceApplication;
    private final AccountService accountService;
    private final String ccsIp;
    private final Map sessions;
    private final ControllerSessionHandler.ShudownAware isShutdown;

    public ConnectionHookConsumer(BlockingQueue<String> queue, String baseUri, String openPath, String closePath, ControllerProxyAndCommandServiceApplication controllerProxyAndCommandServiceApplication, AccountService accountService, String ccsIp, Map sessions, ControllerSessionHandler.ShudownAware isShutdown) {
        this.queue = queue;
        this.baseUri = baseUri;
        this.openPath = openPath;
        this.closePath = closePath;
        this.controllerProxyAndCommandServiceApplication = controllerProxyAndCommandServiceApplication;
        this.accountService = accountService;
        this.ccsIp = ccsIp;
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
                                .post(Entity.json(getPayload(user)));
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

    private Payload getPayload(String username) {
        EntityManager entityManager = controllerProxyAndCommandServiceApplication.createEntityManager();
        User user = accountService.loadByUsername(entityManager, username);
        long controllerId = 0;
        if (user != null && user.getAccount() != null
                && user.getAccount().getControllers() != null
                && user.getAccount().getControllers().get(0) != null) {
            controllerId = user.getAccount().getControllers().get(0).getOid();
        }
        return new Payload(username, controllerId, ccsIp);

    }

}
