package org.openremote.controllercommand.resources;


import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.controllercommand.ControllerProxyAndCommandServiceApplication;
import org.openremote.controllercommand.WSException;
import org.openremote.controllercommand.domain.ControllerCommand;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.domain.ControllerCommandResponseDTO;
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.service.AccountService;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.rest.GenericResourceResultWithErrorMessage;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.websocket.Session;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ControllerSessionHandler {

    protected final static org.slf4j.Logger log = LoggerFactory.getLogger(ControllerSessionHandler.class);

    private ControllerCommandService controllerCommandService;

    private BlockingQueue<String> stateNotificationQueue;

    private ControllerProxyAndCommandServiceApplication controllerProxyAndCommandServiceApplication;
    private AccountService accountService;

    private volatile boolean shutdownInProgress = false;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Payload> connectedControllerByUser = new ConcurrentHashMap<>();
    private final Client client = ClientBuilder.newClient();
    private String ccsIp;
    private String exectuteCommandResponsePath;
    private String baseUri;

    private ControllerSessionHandler() {

    }

    public void prepareConnectionNotification(String baseUri,String executeCommandResponsePath, String openPath, String closePath, String ccsIp, long retryTimeout) {
        this.ccsIp = ccsIp;
        this.baseUri = baseUri;
        this.exectuteCommandResponsePath = executeCommandResponsePath;
        stateNotificationQueue = new ArrayBlockingQueue<>(1000); //TODO check sizing for real usecase
        ConnectionHookConsumer stateConsumer = new ConnectionHookConsumer(stateNotificationQueue, baseUri, openPath, closePath, connectedControllerByUser, sessions, new ShudownAware() {
            @Override
            public boolean isShutdownInProgress() {
                return shutdownInProgress;
            }
        }, retryTimeout);
        Thread thread = new Thread(stateConsumer);
        thread.setDaemon(true);
        thread.start();
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

    public void shutdown() {
        shutdownInProgress = true;
    }

    public Map<String, Payload> getAllConnected() {
        return connectedControllerByUser;
    }

    private static class SingletonHolder {
        private final static ControllerSessionHandler instance = new ControllerSessionHandler();
    }

    public static ControllerSessionHandler getInstance() {
        return SingletonHolder.instance;
    }


    public void addSession(Session session) {

        String username = session.getUserPrincipal().getName();
        /*retrieve openCommandForUser*/
        sessions.put(username, session);
        EntityManager entityManager = controllerProxyAndCommandServiceApplication.createEntityManager();

        Payload payload = getPayload(username, accountService.loadByUsername(entityManager, username));
        connectedControllerByUser.put(username,payload);
        notifyConnection(username);

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

    private void notifyConnection(String user) {
        try {
            stateNotificationQueue.put(user);
        } catch (InterruptedException e) {
            log.error("inQueue interupted", e);
        }
    }


    public void removeSession(Session session) {
        sessions.remove(session.getUserPrincipal().getName());
        notifyConnection(session.getUserPrincipal().getName());
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

        if (controllerCommand.getType() == ControllerCommandDTO.Type.EXECUTE_DEVICE_COMMAND) {
            //send rest to hms
            try {
                Response response = client.target(baseUri)
                        .path(exectuteCommandResponsePath)
                        .request()
                        .post(Entity.json(controllerCommand));
            } catch (Exception ex) {
                log.error("Error trying to submit response for ExecuteDeviceCommand");
            }
        }
    }

    public interface ShudownAware {
       boolean isShutdownInProgress();
    }

    private Payload getPayload(String username, User user) {
        long controllerId = 0;
        if (user != null && user.getAccount() != null
                && user.getAccount().getControllers() != null
                && !user.getAccount().getControllers().isEmpty()
                && user.getAccount().getControllers().get(0) != null) {
            controllerId = user.getAccount().getControllers().get(0).getOid();
        }
        return new Payload(username, controllerId, ccsIp);
    }


}
