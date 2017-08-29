package org.openremote.controllercommand.resources;

import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;


@ServerEndpoint("/ws-commands")
public class WebsocketControllerCommandsResource {

    protected final static org.slf4j.Logger log = LoggerFactory.getLogger(WebsocketControllerCommandsResource.class);

    @OnOpen
    public void open(Session session) {
        log.info("WS["+ session.getId()+"] opening for user : " +  session.getUserPrincipal().getName());
        ControllerSessionHandler.getInstance().addSession(session);
        log.info("WS["+ session.getId()+"] open done for user : " + session.getUserPrincipal().getName());
    }

    @OnClose
    public void close(Session session,CloseReason closeReason) {
        if (closeReason != null) {
            log.info("WS["+ session.getId()+"] closing ["+ closeReason.toString() +"] for user : " + session.getUserPrincipal().getName());
        } else {
            log.info("WS["+ session.getId()+"] closing for user : " + session.getUserPrincipal().getName());
        }
        ControllerSessionHandler.getInstance().removeSession(session);
        log.info("WS["+ session.getId()+"] close done for user : " + session.getUserPrincipal().getName());

    }

    @OnMessage
    public void handleMessage(String message, Session session) {
        log.info("WS["+ session.getId()+"] handling message for user : " + session.getUserPrincipal().getName());
        ControllerSessionHandler.getInstance().handleMessage(message);
        log.info("WS["+ session.getId()+"] message handle for user : " + session.getUserPrincipal().getName());
    }

    @OnError
    public void error(Session session, Throwable error) {
        String username = null;
        if (session != null && session.getUserPrincipal() != null) {
            username = session.getUserPrincipal().getName();
        }
        log.error("WS["+ session.getId()+"] Error for user : " + username, error);
        close(session, new CloseReason( CloseReason.CloseCodes.CLOSED_ABNORMALLY, error.getMessage()));
    }

}
