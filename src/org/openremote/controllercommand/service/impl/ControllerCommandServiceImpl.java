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
package org.openremote.controllercommand.service.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openremote.controllercommand.GenericDAO;
import org.openremote.controllercommand.domain.ControllerCommand;
import org.openremote.controllercommand.domain.InitiateProxyControllerCommand;
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.domain.ControllerCommand.State;
import org.openremote.controllercommand.domain.ControllerCommand.Type;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.springframework.transaction.annotation.Transactional;

/**
 * ControllerCommand service implementation.
 * 
 * @author Stef Epardaud
 */
public class ControllerCommandServiceImpl implements ControllerCommandService {

   protected GenericDAO genericDAO;



   @Override
   @Transactional
   public void save(ControllerCommand c) {
      genericDAO.save(c);
   }

   @Override
   public List<ControllerCommand> queryByUsername(String username){
	   User u = genericDAO.getByNonIdField(User.class, "username", username);
	   if(u == null)
		   return Collections.emptyList();
	   
	   // we want all open controller commands for this account by creation date 
	   DetachedCriteria criteria = DetachedCriteria.forClass(ControllerCommand.class)
	   .add(Restrictions.eq("account", u.getAccount()))
	   .add(Restrictions.eq("state", State.OPEN))
	   .addOrder(Order.asc("creationDate"));
	   
	   List<ControllerCommand> list = genericDAO.findByDetachedCriteria(criteria);
	   return list;
   }


   @Override
   @Transactional
   public InitiateProxyControllerCommand saveProxyControllerCommand(User user, String url) {
      InitiateProxyControllerCommand command = new InitiateProxyControllerCommand(user.getAccount(), Type.INITIATE_PROXY, url);
      save(command);
      return command;
   }



   @Override
   public void closeControllerCommand(ControllerCommand controllerCommand) {
      controllerCommand.setState(State.DONE);
   }

   @Override
   @Transactional
   public ControllerCommand findControllerCommandById(Long id) {
      return genericDAO.getById(ControllerCommand.class, id);
   }

   @Override
   @Transactional
   public void update(ControllerCommand controllerCommand) {
      genericDAO.saveOrUpdate(controllerCommand);
   }

   //
   // Internal plumbing
   public void setGenericDAO(GenericDAO genericDAO) {
     this.genericDAO = genericDAO;
  }

}
