package org.openremote.controllercommand.resources;

import flexjson.JSONSerializer;
import org.openremote.beehive.EntityTransactionFilter;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.rest.GenericResourceResultWithErrorMessage;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:eric@openremote.org">Eric Bariaux</a>
 */
@Path("/")
public class ControllerCommandsResource
{
  @Inject
  private ControllerCommandService controllerCommandService;

  /**
   * Return a list of all not finished ControllerCommands<p>
   * REST Url: /rest/commands/{controllerOid} -> return all not finished controller commands for the given controllerOid
   * 
   * @return a List of ControllerCommand
   */

  @GET
  @Path("commands/{controllerOid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response loadControllerCommands(@Context HttpServletRequest request, @PathParam("controllerOid") String controllerOid)
  {
    GenericResourceResultWithErrorMessage result = null;
    try
    {
      if (controllerOid != null)
      {
        Long id = Long.valueOf(controllerOid);
        
        String username = request.getUserPrincipal().getName();

        List<ControllerCommandDTO> commands = controllerCommandService.queryByControllerOidForUser(getEntityManager(request), id, username);
        result = new GenericResourceResultWithErrorMessage(null, commands);
      } else {
        result = new GenericResourceResultWithErrorMessage(null, new ArrayList<ControllerCommandDTO>());
      }
    } catch (Exception e)
    {
      result = new GenericResourceResultWithErrorMessage(e.getMessage(), null);
    }
    return Response.ok(new JSONSerializer().exclude("*.class").deepSerialize(result)).build();
  }

  private EntityManager getEntityManager(HttpServletRequest request)
  {
    return (EntityManager)request.getAttribute(EntityTransactionFilter.PERSISTENCE_ENTITY_MANAGER_LOOKUP);
  }

}
