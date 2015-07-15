/* OpenRemote, the Home of the Digital Home.
* Copyright 2008-2011, OpenRemote Inc.
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
package org.openremote.controllercommand.service;

import java.util.List;

import org.openremote.controllercommand.domain.ControllerCommand;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.domain.InitiateProxyControllerCommand;
import org.openremote.controllercommand.domain.User;

/**
 * Account service.
 * 
 * @author Stef Epardaud
 *
 */
public interface ControllerCommandService {
   
   void save(ControllerCommand controllerCommand);

   void update(ControllerCommand controllerCommand);

   void closeControllerCommand(ControllerCommand controllerCommand);

   ControllerCommand findControllerCommandById(Long id);

   InitiateProxyControllerCommand saveProxyControllerCommand(User user, String url);

  List<ControllerCommandDTO> queryByControllerOid(Long oid);

}
