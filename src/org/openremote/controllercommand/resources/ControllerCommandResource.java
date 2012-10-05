package org.openremote.controllercommand.resources;

import org.openremote.controllercommand.domain.ControllerCommand;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.rest.GenericResourceResultWithErrorMessage;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.ServerResource;

import flexjson.JSONSerializer;

public class ControllerCommandResource extends ServerResource
{

  private ControllerCommandService controllerCommandService;

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
  
  

  public void setControllerCommandService(ControllerCommandService controllerCommandService)
  {
    this.controllerCommandService = controllerCommandService;
  }

}
