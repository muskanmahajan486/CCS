package org.openremote.controllercommand.resources;


import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.controllercommand.ControllerProxyAndCommandServiceApplication;
import org.openremote.controllercommand.WSException;
import org.openremote.controllercommand.domain.ControllerCommand;
import org.openremote.controllercommand.domain.ControllerCommandResponseDTO;
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.service.AccountService;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.rest.GenericResourceResultWithErrorMessage;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@ApplicationScoped
public class ControllerSessionHandler {

    private ControllerCommandService controllerCommandService;

    private BlockingQueue<String> openConnectionQueue;
    private BlockingQueue<String> closeConnectionQueue;

    private ControllerProxyAndCommandServiceApplication controllerProxyAndCommandServiceApplication;

    protected final static org.slf4j.Logger log = LoggerFactory.getLogger(ControllerSessionHandler.class);
    private AccountService accountService;

    private ControllerSessionHandler() {

    }

    public void prepareConnectionNotification(String baseUri, String openPath, String closePath) {
        openConnectionQueue = new ArrayBlockingQueue<>(1000);
        closeConnectionQueue = new ArrayBlockingQueue<>(1000);
        ConnectionHookConsumer openConsumer = new ConnectionHookConsumer(openConnectionQueue, baseUri, openPath);
        ConnectionHookConsumer closeConsumer = new ConnectionHookConsumer(closeConnectionQueue, baseUri, closePath);

        new Thread(openConsumer).start();
        new Thread(closeConsumer).start();
    }


    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    public void setControllerCommandService(ControllerCommandService controllerCommandService) {
        this.controllerCommandService = controllerCommandService;
    }

    public void setEntityFilter(ControllerProxyAndCommandServiceApplication controllerProxyAndCommandServiceApplication) {
        this.controllerProxyAndCommandServiceApplication = controllerProxyAndCommandServiceApplication;
    }

    public Collection<Session> getSessions() {
        return this.sessions.values();
    }

    public boolean hasSession(String username) {
        return this.sessions.containsKey(username);
    }


    private static class SingletonHolder {
        private final static ControllerSessionHandler instance = new ControllerSessionHandler();
    }

    public static ControllerSessionHandler getInstance() {
        return SingletonHolder.instance;
    }

    private final Map<String, Session> sessions = new HashMap<>();

    public void addSession(Session session) {
        String username = session.getUserPrincipal().getName();

        sessions.put(username, session);
        //retrieve openCommandForUser
        EntityManager entityManager = controllerProxyAndCommandServiceApplication.createEntityManager();

        try {
            openConnectionQueue.put(username);
        } catch (InterruptedException e) {
            log.error("inQueue interupted", e);
        }

        List<ControllerCommand> openCommands = controllerCommandService.findControllerCommandByStatusAndUsername(entityManager, ControllerCommand.State.OPEN, username);
        for (ControllerCommand openCommand : openCommands) {
            try {
                sendToController(username, openCommand);
            } catch (WSException e) {
                log.info("Error trying to send OPEN Controller Command", e);
            }
        }
        controllerProxyAndCommandServiceApplication.commitEntityManager(entityManager);

    }

    private void notifyOpenConnection(User user) {
        log.info("Send OpenConnection");
    }

    private void notifyCloseConnection(User user) {
        log.info("Send CloseConnection");
    }


    public void removeSession(Session session) {
        if (!openConnectionQueue.remove(session.getUserPrincipal().getName())) {
            try {
                closeConnectionQueue.put(session.getUserPrincipal().getName());
            } catch (InterruptedException e) {
                log.error("closeQueue interupted", e);
            }
        }
        sessions.remove(session.getUserPrincipal().getName());


    }

    public void sendToController(String username, ControllerCommand command) throws WSException {
        GenericResourceResultWithErrorMessage resultForWS = new GenericResourceResultWithErrorMessage(null, ControllerCommandService.getControllerCommandDTO(command));
        try {
            sendToController(username, new JSONObject(new JSONSerializer().exclude("*.class").deepSerialize(resultForWS)));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendToController(String username, JSONObject message) throws WSException {

        Session session = sessions.get(username);
        if (session != null && session.isOpen()) {
            sendToSession(session, message);
        } else {
            if (session != null) {
                removeSession(session);
            }
            throw new WSException("No session for user : " + username);
        }
    }


    private void sendToSession(Session session, JSONObject message) throws WSException {
        try {
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException ex) {
            removeSession(session);
            throw new WSException(ex);
        }
    }


    public void handleMessage(String message) {
        EntityManager entityManager = controllerProxyAndCommandServiceApplication.createEntityManager();
        ControllerCommandResponseDTO res = new JSONDeserializer<ControllerCommandResponseDTO>()
                .use(null, ControllerCommandResponseDTO.class)
                .deserialize(message);

        ControllerCommand controllerCommand = controllerCommandService.findControllerCommandById(entityManager, res.getOid());
        if (res.getCommandTypeEnum().equals(ControllerCommandResponseDTO.Type.SUCCESS)) {
            controllerCommandService.closeControllerCommand(controllerCommand);
        } else {
            controllerCommandService.markFailedControllerCommand(controllerCommand);
        }
        try {
            controllerCommandService.update(entityManager, controllerCommand);
            controllerProxyAndCommandServiceApplication.commitEntityManager(entityManager);
        } catch (Exception ex) {
            controllerProxyAndCommandServiceApplication.rollbackEntityManager(entityManager);
        }
        controllerProxyAndCommandServiceApplication.commitEntityManager(entityManager);
    }


}
