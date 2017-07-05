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
import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.beehive.EntityTransactionFilter;
import org.openremote.controllercommand.WSException;
import org.openremote.controllercommand.domain.*;
import org.openremote.controllercommand.domain.ControllerCommandDTO.Type;
import org.openremote.controllercommand.service.AccountService;
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

@Path("/")
public class ControllerCommandResource {
    @Inject
    private ControllerCommandService controllerCommandService;

    @Inject
    private ControllerSessionHandler controllerSessionHandler;


    @Inject
    private AccountService accountService;

    protected final static Logger log = LoggerFactory.getLogger(ControllerCommandResource.class);

    /**
     * Mark the controllerCommand with the given id as DONE<p>
     * REST Url: /rest/command/{commandId}
     *
     * @return ResultObject with String "ok"
     */
    @DELETE
    @Path("command/{commandId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response ackControllerCommands(@Context HttpServletRequest request, @PathParam("commandId") String commandId) {
        GenericResourceResultWithErrorMessage result = null;
        try {
            if (commandId != null) {
                log.info("Asked to acknowledge command with id " + commandId);
                Long id = Long.valueOf(commandId);
                ControllerCommand controllerCommand = controllerCommandService.findControllerCommandById(getEntityManager(request), id);
                controllerCommandService.closeControllerCommand(controllerCommand);
                controllerCommandService.update(getEntityManager(request), controllerCommand);
                result = new GenericResourceResultWithErrorMessage(null, "ok");
            } else {
                result = new GenericResourceResultWithErrorMessage("command not found", null);
            }
        } catch (Exception e) {
            log.error("Error while acknowledging a ControllerCommand", e);
            result = new GenericResourceResultWithErrorMessage(e.getMessage(), null);
        }
        return Response.ok(new JSONSerializer().exclude("*.class").deepSerialize(result)).build();
    }

    @POST
    @Path("command")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveCommand(@Context HttpServletRequest request, String jsonString) {
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
                if (Type.DOWNLOAD_DESIGN != type && Type.EXECUTE_DEVICE_COMMAND != type) {
                    throw new BadRequestException("Unsupported command type from user : " + username);
                }
                ControllerCommand command;
                if (Type.EXECUTE_DEVICE_COMMAND == type) {
                    String deviceName = jsonData.getString("deviceName");
                    String commandName = jsonData.getString("commandName");
                    String parameter = jsonData.getString("parameter");
                    command = new ExecuteDeviceControllerCommand(account, type, deviceName, commandName, parameter);
                } else {
                    command = new ControllerCommand(account, type);

                }
                controllerCommandService.save(getEntityManager(request), command);

                GenericResourceResultWithErrorMessage result = new GenericResourceResultWithErrorMessage(null, command);
                try {
                    controllerSessionHandler.sendToController(user.getUsername(), command);
                } catch (WSException e) {
                    log.info("Error on sending WS command, username: " + username, e);
                }
                return Response.ok(new JSONSerializer().exclude("*.class").exclude("result.account").deepSerialize(result)).build();
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown command type from user : " + username);
            }
        } catch (JSONException e) {
            throw new BadRequestException("Invalid JSON payload from user : " + username);
        }
    }

    @GET
    @Path("command/{controllerCommandOid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadControllerCommands(@Context HttpServletRequest request, @PathParam("controllerCommandOid") String controllerCommandOid) {
        GenericResourceResultWithErrorMessage result = null;
        try {

            if (controllerCommandOid != null) {
                log.info("Asked to get command id " + controllerCommandOid);
                Long id = Long.valueOf(controllerCommandOid);

                String username = request.getUserPrincipal().getName();
                log.info("Query done by " + username);

                ControllerCommand command = controllerCommandService.findControllerCommandById(getEntityManager(request), id);
                if (username.equals(command.getAccount().getUsers().get(0).getUsername())) {
                    result = new GenericResourceResultWithErrorMessage(null, command);
                } else {
                    log.info("Username mismatch");
                    result = new GenericResourceResultWithErrorMessage("User is not allowed to retrieve command +" + id, null);

                }
            } else {
                log.info("No command oid provided");
                result = new GenericResourceResultWithErrorMessage(null, null);
            }
        } catch (Exception e) {
            log.error("Error getting controller commands", e);
            result = new GenericResourceResultWithErrorMessage(e.getMessage(), null);
        }
        return Response.ok(new JSONSerializer().exclude("*.class").serialize(result)).build();
    }

    private EntityManager getEntityManager(HttpServletRequest request) {
        return (EntityManager) request.getAttribute(EntityTransactionFilter.PERSISTENCE_ENTITY_MANAGER_LOOKUP);
    }

}
