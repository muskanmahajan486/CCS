package org.openremote.controllercommand.resources;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.controllercommand.domain.Account;
import org.openremote.controllercommand.domain.ControllerCommand;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.domain.ControllerCommandDTO.Type;
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.service.AccountService;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.rest.GenericResourceResultWithErrorMessage;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import flexjson.JSONSerializer;

public class ControllerCommandResource extends ServerResource
{

  private ControllerCommandService controllerCommandService;
  
  private AccountService accountService;

  /**
   * Mark the controllerCommand with the given id as DONE<p>
   * REST Url: /rest/command/{commandId} 
   * 
   * @return ResultObject with String "ok"
   */
  @Delete("json")
  public Representation ackControllerCommands()
  {
    GenericResourceResultWithErrorMessage result = null;
    try
    {
      String oid = (String) getRequest().getAttributes().get("commandId");
      if (oid != null)
      {
        Long id = Long.valueOf(oid);
        ControllerCommand controllerCommand = controllerCommandService.findControllerCommandById(id);
        controllerCommandService.closeControllerCommand(controllerCommand);
        controllerCommandService.update(controllerCommand);
        result = new GenericResourceResultWithErrorMessage(null, "ok");
      } else {
        result = new GenericResourceResultWithErrorMessage("command not found", null);
      }
    } catch (Exception e)
    {
      result = new GenericResourceResultWithErrorMessage(e.getMessage(), null);
    }
    Representation rep = new JsonRepresentation(new JSONSerializer().exclude("*.class").deepSerialize(result));
    return rep;
  }
  
  @Post("json:json")
  public Representation saveCommand(Representation data)
  {
    if (data == null) {
      return generateErrorResponse(Status.CLIENT_ERROR_BAD_REQUEST, "No data received");
    }
    if (!MediaType.APPLICATION_JSON.equals(data.getMediaType(), true)) {
      return generateErrorResponse(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE, "Only JSON payload are supported");
    }
    
    Request restletRequest = getRequest();
    HttpServletRequest servletRequest = ServletUtils.getRequest(restletRequest);
    String username = servletRequest.getUserPrincipal().getName();
    
    User user = accountService.loadByUsername(username);
    Account account = user.getAccount();
    
    Representation rep = null;
    try {
      JSONObject jsonData = new JSONObject(data.getText());
      String typeAsString = jsonData.getString("type");
      if (typeAsString == null) {
        return generateErrorResponse(Status.CLIENT_ERROR_BAD_REQUEST, "Type must be provided");
      }
      try {
        ControllerCommandDTO.Type type = Type.valueOf(typeAsString.trim().toUpperCase());
        if (Type.DOWNLOAD_DESIGN != type) {
          return generateErrorResponse(Status.CLIENT_ERROR_BAD_REQUEST, "Unsupported command type");
        }
        ControllerCommand command = new ControllerCommand(account, Type.DOWNLOAD_DESIGN);
        controllerCommandService.save(command);
        GenericResourceResultWithErrorMessage result = new GenericResourceResultWithErrorMessage(null, command);
        rep = new JsonRepresentation(new JSONSerializer().exclude("*.class").exclude("result.account").deepSerialize(result));
      } catch (IllegalArgumentException e) {
        return generateErrorResponse(Status.CLIENT_ERROR_BAD_REQUEST, "Unknown command type");            
      }
    } catch (JSONException e) {
      return generateErrorResponse(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid JSON payload");
    } catch (IOException e1) {
      return generateErrorResponse(Status.SERVER_ERROR_INTERNAL, "Can't read payload");
    }
    
    return rep;
  }
  
  private Representation generateErrorResponse(Status status, String errorMessage)
  {
    GenericResourceResultWithErrorMessage result = new GenericResourceResultWithErrorMessage(errorMessage, null);
    getResponse().setStatus(status);
    return new JsonRepresentation(new JSONSerializer().exclude("*.class").deepSerialize(result));
  }
  

  public void setControllerCommandService(ControllerCommandService controllerCommandService)
  {
    this.controllerCommandService = controllerCommandService;
  }

  public void setAccountService(AccountService accountService)
  {
    this.accountService = accountService;
  }
  
}
