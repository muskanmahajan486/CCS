package org.openremote.controllercommand.resources;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.openremote.controllercommand.GenericDAO;
import org.openremote.controllercommand.domain.DiscoveredDevice;
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
        long id = Long.parseLong(oid);
//        search.add(Restrictions.eq("oid", id));
//        List<DiscoveredDevice> devices = dao.findByDetachedCriteria(search);
//        result = new GenericResourceResultWithErrorMessage(null, devices);
        result = new GenericResourceResultWithErrorMessage(null, new ArrayList<String>());
      } else {
        result = new GenericResourceResultWithErrorMessage(null, new ArrayList<String>());
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
