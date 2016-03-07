package org.openremote.controllercommand.resources;

import flexjson.JSONSerializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.beehive.EntityTransactionFilter;
import org.openremote.controllercommand.domain.Account;
import org.openremote.controllercommand.domain.ControllerCommand;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.domain.ControllerCommandDTO.Type;
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.service.AccountService;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.controllercommand.service.impl.AccountServiceImpl;
import org.openremote.controllercommand.service.impl.ControllerCommandServiceImpl;
import org.openremote.rest.GenericResourceResultWithErrorMessage;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class ControllerCommandResource
{
  @Inject
  private ControllerCommandServiceImpl controllerCommandService;

  @Inject
  private AccountServiceImpl accountService;

  /**
   * Mark the controllerCommand with the given id as DONE<p>
   * REST Url: /rest/command/{commandId} 
   * 
   * @return ResultObject with String "ok"
   */
  @DELETE
  @Path("command/{commandId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response ackControllerCommands(@Context HttpServletRequest request, @PathParam("commandId") String commandId)
  {
    GenericResourceResultWithErrorMessage result = null;
    try
    {
      if (commandId != null)
      {
        Long id = Long.valueOf(commandId);
        ControllerCommand controllerCommand = controllerCommandService.findControllerCommandById(getEntityManager(request), id);
        controllerCommandService.closeControllerCommand(controllerCommand);
        controllerCommandService.update(getEntityManager(request), controllerCommand);
        result = new GenericResourceResultWithErrorMessage(null, "ok");
      } else {
        result = new GenericResourceResultWithErrorMessage("command not found", null);
      }
    } catch (Exception e)
    {
      result = new GenericResourceResultWithErrorMessage(e.getMessage(), null);
    }
    return Response.ok(new JSONSerializer().exclude("*.class").deepSerialize(result)).build();
  }

  @POST
  @Path("command")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response saveCommand(@Context HttpServletRequest request, String jsonString)
  {
    if (jsonString == null) {
      throw new BadRequestException("No data received");
    }

    String username = request.getUserPrincipal().getName();
    
    User user = accountService.loadByUsername(getEntityManager(request), username);
    Account account = user.getAccount();
    
    try {
      JSONObject jsonData = new JSONObject(jsonString);
      String typeAsString = jsonData.getString("type");
      if (typeAsString == null) {
        throw new BadRequestException("Type must be provided");
      }
      try {
        ControllerCommandDTO.Type type = Type.valueOf(typeAsString.trim().toUpperCase());
        if (Type.DOWNLOAD_DESIGN != type) {
          throw new BadRequestException("Unsupported command type");
        }
        ControllerCommand command = new ControllerCommand(account, Type.DOWNLOAD_DESIGN);
        controllerCommandService.save(getEntityManager(request), command);
        GenericResourceResultWithErrorMessage result = new GenericResourceResultWithErrorMessage(null, command);
        return Response.ok(new JSONSerializer().exclude("*.class").exclude("result.account").deepSerialize(result)).build();
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Unknown command type");
      }
    } catch (JSONException e) {
      throw new BadRequestException("Invalid JSON payload");
    }
  }

  private EntityManager getEntityManager(HttpServletRequest request)
  {
    return (EntityManager)request.getAttribute(EntityTransactionFilter.PERSISTENCE_ENTITY_MANAGER_LOOKUP);
  }

}
