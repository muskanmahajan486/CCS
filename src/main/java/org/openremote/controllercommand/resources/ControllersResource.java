/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2016, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.controllercommand.resources;

import flexjson.JSONSerializer;
import org.openremote.beehive.EntityTransactionFilter;
import org.openremote.controllercommand.domain.Controller;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.openremote.rest.GenericResourceResultWithErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;


@Path("/")
public class ControllersResource
{
  @Inject
  private ControllerCommandService controllerCommandService;

  @Inject
  private ControllerSessionHandler sessionHandler;

  protected final static Logger log = LoggerFactory.getLogger(ControllersResource.class);

  /**
   * Return a list of all not finished ControllerCommands<p>
   * REST Url: /rest/commands/{controllerOid} -> return all not finished controller commands for the given controllerOid
   * 
   * @return a List of ControllerCommand
   */

  @GET
  @Path("controllers")
  @Produces(MediaType.APPLICATION_JSON)
  public Response loadConnectedControllers(@Context HttpServletRequest request, @QueryParam("username") String username)
  {

    //TODO : getAll session, if username get only username session
    GenericResourceResultWithErrorMessage result = null;
    try
    {
        result = new GenericResourceResultWithErrorMessage(null, "ok");
    } catch (Exception e)
    {
      log.error("Error getting controllers", e);
      result = new GenericResourceResultWithErrorMessage(e.getMessage(), null);
    }
    return Response.ok(new JSONSerializer().exclude("*.class").deepSerialize(result)).build();
  }


  @GET
  @Path("controllers/{controllerId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response loadConnectedControllers(@Context HttpServletRequest request, @PathParam("controllerId") Long controllerId) {
    //TODO : get session for controllerId
    GenericResourceResultWithErrorMessage result = new GenericResourceResultWithErrorMessage(null, "hahaha");
    return Response.ok(new JSONSerializer().exclude("*.class").deepSerialize(result)).build();
  }




}
