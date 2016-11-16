package org.openremote.controllercommand.resources;

import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;


@ServerEndpoint("/ws-commands")
public class WebsocketControllerCommandsResource {


    protected final static org.slf4j.Logger log = LoggerFactory.getLogger(WebsocketControllerCommandsResource.class);


    @OnOpen
    public void open(Session session) {
        log.debug("Session:", session);
        ControllerSessionHandler.getInstance().addSession(session);
    }

    @OnClose
    public void close(Session session) {
        log.debug("Session:", session);
        ControllerSessionHandler.getInstance().removeSession(session);
    }

    @OnError
    public void onError(Throwable error) {
        log.debug("Error:", error);

    }

    @OnMessage
    public void handleMessage(String message, Session session) {
        log.debug("Message:", message);
        ControllerSessionHandler.getInstance().handleMessage(message,session);

    }

}
