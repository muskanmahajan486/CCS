package org.openremote.controllercommand.resources;

import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;


@ServerEndpoint("/ws-commands")
public class WebsocketControllerCommandsResource {

    protected final static org.slf4j.Logger log = LoggerFactory.getLogger(WebsocketControllerCommandsResource.class);

    @OnOpen
    public void open(Session session) {
        ControllerSessionHandler.getInstance().addSession(session);
    }

    @OnClose
    public void close(Session session) {
        ControllerSessionHandler.getInstance().removeSession(session);
    }

    @OnMessage
    public void handleMessage(String message, Session session) {
        ControllerSessionHandler.getInstance().handleMessage(message);
    }

    @OnError
    public void error(Session session, Throwable error) {
        log.error("WS Error",error);
        close(session);
    }

}
