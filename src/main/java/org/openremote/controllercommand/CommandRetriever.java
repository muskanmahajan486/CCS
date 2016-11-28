package org.openremote.controllercommand;

import flexjson.JSONSerializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.controllercommand.domain.ControllerCommand;
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.resources.ControllerCommandResource;
import org.openremote.controllercommand.resources.ControllerSessionHandler;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.rest.GenericResourceResultWithErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.List;

public class CommandRetriever extends Thread {

   private ControllerProxyAndCommandServiceApplication controllerProxyAndCommandServiceApplication;
   private ControllerCommandService controllerCommandService;
   private ControllerSessionHandler controllerSessionHandler;

   protected final static Logger log = LoggerFactory.getLogger(ControllerCommandResource.class);

   public CommandRetriever(ControllerProxyAndCommandServiceApplication app, ControllerCommandService controllerCommandService, ControllerSessionHandler controllerSessionHandler) {
      this.controllerProxyAndCommandServiceApplication = app;
      this.controllerCommandService = controllerCommandService;
      this.controllerSessionHandler = controllerSessionHandler;
   }

   @Override
   public void run() {
      boolean interrupted = false;
      while (!interrupted) {
         EntityManager entityManager = controllerProxyAndCommandServiceApplication.createEntityManager();
         List<ControllerCommand> controllerCommands = controllerCommandService.findControllerCommandByStatus(entityManager, ControllerCommand.State.FAILED);
         for (ControllerCommand command : controllerCommands) {
            for (User user : command.getAccount().getUsers()) {
               if (controllerSessionHandler.hasSession(user.getUsername())) {
                  try {
                     controllerSessionHandler.sendToController(user.getUsername(), command);
                  } catch (WSException e) {
                     log.info("Error on sending WS command",e);
                  }
               }
            }
         }

         try {
            Thread.sleep(30000);
         } catch (InterruptedException e) {
            interrupted = true;
         }

      }
   }
}
