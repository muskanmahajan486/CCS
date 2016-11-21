package org.openremote.controllercommand.resources;


import org.json.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ControllerSessionHandler {

    private ControllerSessionHandler() {}

    private static class SingletonHolder
    {
        private final static ControllerSessionHandler instance = new ControllerSessionHandler();
    }

    public static ControllerSessionHandler getInstance()
    {
        return SingletonHolder.instance;
    }

    private final Map<String,Session> sessions = new HashMap<>();

    public void addSession(Session session) {
        sessions.put(session.getUserPrincipal().getName(), session);
    }

    public void removeSession(Session session) {
        sessions.remove(session.getUserPrincipal().getName());
    }

    public void sendToController(String username, JSONObject message) {

        Session session = sessions.get(username);
        if (session != null && session.isOpen()) {
            sendToSession(session, message);
        } else {
            if (session != null) {
                removeSession(session);
            }
            Logger.getLogger(ControllerSessionHandler.class.getName()).log(Level.WARNING, null, "No session for user : " + username);
        }
    }


    private void sendToSession(Session session, JSONObject message) {
        try {
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException ex) {
            removeSession(session);
            Logger.getLogger(ControllerSessionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public void handleMessage(String message, Session session) {
        //TODO: handle message from controller ?
        //remove message in db
        //close command


    }

}
