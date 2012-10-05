package org.openremote.controllercommand.domain;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import flexjson.JSON;

@Entity
@Table(name = "discovered_device")
public class DiscoveredDevice extends BusinessEntity
{
  private static final long serialVersionUID = -4324310503187625990L;

  /** The device name. */
  private String name;

  /** The device's model. */
  private String model;
  
  /** The OpenRemote protocol which is responsible to control this devices */
  private String protocol;

  /** The device type eg. Switch, Dimmer, Thermometer, TV, .... */
  private String type;
  
  /** Is this device already used in the Building Modeler */ 
  private Boolean used;

  /** The device attrs. */
  private List<DiscoveredDeviceAttr> deviceAttrs;

  /** The account whose controller announced this device */
  private Account account;

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getModel()
  {
    return model;
  }

  public void setModel(String model)
  {
    this.model = model;
  }

  public String getProtocol()
  {
    return protocol;
  }

  public void setProtocol(String protocol)
  {
    this.protocol = protocol;
  }

  public String getType()
  {
    return type;
  }

  public void setType(String type)
  {
    this.type = type;
  }

  /**
   * Gets the device attrs.
   * 
   * @return the device attrs
   */
  @OneToMany(mappedBy = "discoveredDevice", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  public List<DiscoveredDeviceAttr> getDeviceAttrs()
  {
    return deviceAttrs;
  }

  /**
   * Sets the device attrs.
   * 
   * @param deviceAttrs
   *          the new device attrs
   */
  public void setDeviceAttrs(List<DiscoveredDeviceAttr> deviceAttrs)
  {
    this.deviceAttrs = deviceAttrs;
  }

  /**
   * Gets the account.
   * 
   * @return the account
   */
  @ManyToOne
  @JSON(include = false)
  public Account getAccount()
  {
    return account;
  }

  /**
   * Sets the account.
   * 
   * @param account
   *          the new account
   */
  public void setAccount(Account account)
  {
    this.account = account;
  }

  public Boolean getUsed()
  {
    return used;
  }

  public void setUsed(Boolean used)
  {
    this.used = used;
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
    DiscoveredDevice other = (DiscoveredDevice) obj;
    return other.getOid() == getOid();
  }
}
