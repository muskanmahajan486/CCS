package org.openremote.controllercommand.resources;

import java.util.ArrayList;
import java.util.List;

import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.rest.GenericResourceResultWithErrorMessage;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import flexjson.JSONSerializer;

public class ControllerCommandsResource extends ServerResource
{

  private ControllerCommandService controllerCommandService;

  /**
   * Return a list of all not finished ControllerCommands<p>
   * REST Url: /rest/commands/{controllerOid} -> return all not finished controller commands for the given controllerOid
   * 
   * @return a List of ControllerCommand
   */
  @Get("json")
  public Representation loadControllerCommands()
  {
    GenericResourceResultWithErrorMessage result = null;
    try
    {
      String oid = (String) getRequest().getAttributes().get("controllerOid");
      if (oid != null)
      {
        Long id = Long.valueOf(oid);
        List<ControllerCommandDTO> commands = controllerCommandService.queryByControllerOid(id);
        result = new GenericResourceResultWithErrorMessage(null, commands);
      } else {
        result = new GenericResourceResultWithErrorMessage(null, new ArrayList<ControllerCommandDTO>());
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
