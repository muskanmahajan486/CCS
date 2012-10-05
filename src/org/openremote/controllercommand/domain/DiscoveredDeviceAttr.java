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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;



/**
 * The Attribute of Device.
 * 
 * @author Marcus
 */
@Entity
@Table(name = "discovered_device_attr")
public class DiscoveredDeviceAttr extends BusinessEntity {

   private static final long serialVersionUID = 5667820127033318691L;

   /** The name. */
   private String name;
   
   /** The value. */
   private String value;
   
   /** The device. */
   private DiscoveredDevice discoveredDevice;
   
   /**
    * Gets the name.
    * 
    * @return the name
    */
   @Column(nullable = false)
   public String getName() {
      return name;
   }

   /**
    * Sets the name.
    * 
    * @param name the new name
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * Gets the value.
    * 
    * @return the value
    */
   @Column(nullable = false)
   public String getValue() {
      return value;
   }

   /**
    * Sets the value.
    * 
    * @param value the new value
    */
   public void setValue(String value) {
      this.value = value;
   }

   /**
    * Gets the discoveredDevice.
    * 
    * @return the discoveredDevice
    */
   @ManyToOne
   @JoinColumn(nullable = false)
   public DiscoveredDevice getDiscoveredDevice() {
      return discoveredDevice;
   }

   /**
    * Sets the discoveredDevice.
    * 
    * @param discoveredDevice the new discoveredDevice
    */
   public void setDiscoveredDevice(DiscoveredDevice discoveredDevice) {
      this.discoveredDevice = discoveredDevice;
   }

}
