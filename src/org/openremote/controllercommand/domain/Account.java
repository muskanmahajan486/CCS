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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * The Class Account.
 * 
 * @author Marcus
 */
@Entity
@Table(name = "account")
public class Account extends BusinessEntity
{

  private static final long serialVersionUID = -5029444774173816237L;

  /** The users. */
  private List<User> users;

  /** The devices. */
  private List<DiscoveredDevice> discoverdDevices;

  /**
   * Instantiates a new account.
   */
  public Account()
  {
    discoverdDevices = new ArrayList<DiscoveredDevice>();
  }

  @OneToMany(mappedBy = "account")
  public List<User> getUsers()
  {
    return users;
  }

  public void setUsers(List<User> users)
  {
    this.users = users;
  }

  @OneToMany(mappedBy = "account")
  public List<DiscoveredDevice> getDiscoverdDevices()
  {
    return discoverdDevices;
  }

  public void setDiscoverdDevices(List<DiscoveredDevice> discoverdDevices)
  {
    this.discoverdDevices = discoverdDevices;
  }

  /**
   * Adds a discovered device.
   * 
   * @param device
   *          the discovered device
   */
  public void addDevice(DiscoveredDevice device)
  {
    discoverdDevices.add(device);
  }

  @Override
  public int hashCode()
  {
    return (int) getOid();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Account other = (Account) obj;
    return other.getOid() == getOid();
  }
}
