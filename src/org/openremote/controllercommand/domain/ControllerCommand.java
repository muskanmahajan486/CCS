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
package org.openremote.controllercommand.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity for commands sent from Beehive to the backend
 * 
 * @author Stphane Epardaud <stef@epardaud.fr>
 */
@SuppressWarnings("serial")
@Entity 
@Table(name="controller_command")
@Inheritance(strategy = InheritanceType.JOINED)
public class ControllerCommand extends BusinessEntity {

	private Account account;
	private Date creationDate;
	private State state;
	private Type type;
	
	/**
	 * The type of command. This is required because not every command needs a subclass
	 * from which we can infer the type from.
	 */
	public enum Type {
	   UPLOAD_LOGS, UPDATE_CONTROLLER, INITIATE_PROXY;

	   /**
	    * Gets a label for this type, this is useful for exporting. Label format is
	    * name().replace("_", "-").toLowerCase().
	    */
      public String getLabel() {
         return name().replace("_", "-").toLowerCase();
      }

      /**
       * Gets a type from a label, this is useful for importing. Label format is
       * name().replace("_", "-").toLowerCase().
       */
      public static Type fromLabel(String label) {
         return valueOf(label.replace("-", "_").toUpperCase());
      }
	}
	
	/**
	 * State of the command. They start as OPEN, then are marked as DONE when they have been treated by
	 * the controller.
	 */
	public enum State {
		OPEN, DONE;
	}

	public ControllerCommand() {
	}

	public ControllerCommand(Account account, Type type) {
		this.account = account;
		this.creationDate = new Date();
		this.state = State.OPEN;
		this.type = type;
	}

	@ManyToOne
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	@Enumerated(EnumType.STRING)
	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

   @Enumerated(EnumType.STRING)
   public Type getType() {
      return type;
   }

   public void setType(Type type) {
      this.type = type;
   }
}
