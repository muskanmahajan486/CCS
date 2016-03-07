package org.openremote.controllercommand;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerConfig;
import org.openremote.controllercommand.proxy.ProxyServer;
import org.openremote.controllercommand.resources.ControllerCommandResource;
import org.openremote.controllercommand.resources.ControllerCommandsResource;
import org.openremote.controllercommand.service.impl.AccountServiceImpl;
import org.openremote.controllercommand.service.impl.ControllerCommandServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

public class ControllerProxyAndCommandServiceApplication extends ResourceConfig
{
  // TODO: get context name from init param
  static private EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("CCS-MySQL");

  protected final static Logger persistenceLog = LoggerFactory.getLogger(ControllerProxyAndCommandServiceApplication.class);

  public ControllerProxyAndCommandServiceApplication()
  {
    GenericDAO genericDAO = new GenericDAO();
    final AccountServiceImpl accountService = new AccountServiceImpl();
    accountService.setGenericDAO(genericDAO);
    final ControllerCommandServiceImpl controllerCommandService = new ControllerCommandServiceImpl();
    controllerCommandService.setGenericDAO(genericDAO);

    Properties config = new Properties();
    try
    {
      config.load(getClassLoader().getResourceAsStream("config.properties"));
    } catch (IOException e)
    {
      // TODO: log
      e.printStackTrace();
    }

    String proxyHostname = config.getProperty("proxy.hostname", "localhost");
    Integer proxyTimeout = getIntegerConfiguration(config, "proxy.timeout", 10000);
    Integer proxyPort = getIntegerConfiguration(config, "proxy.port", 10000);
    String proxyClientPortRange = config.getProperty("proxy.clientPortRange", "30000-30010");
    Boolean useSSL = getBooleanConfiguration(config, "proxy.useSSL", true);
    String keystore = config.getProperty("proxy.keystore", "keystore.ks");
    String keystorePassword = config.getProperty("proxy.keystorePassword", "storepass");
    ProxyServer ps = new ProxyServer(proxyHostname, proxyTimeout, proxyPort, proxyClientPortRange, useSSL, keystore, keystorePassword, controllerCommandService, accountService, this);
    ps.start();

    register(ControllerCommandResource.class);
    register(ControllerCommandsResource.class);

    register(new org.glassfish.hk2.utilities.binding.AbstractBinder()
             {
               @Override
               protected void configure()
               {
                 bind(accountService);
                 bind(controllerCommandService);
               }
             });

  }

  private Integer getIntegerConfiguration(Properties config, String propertyName, Integer defaultValue) {
    String stringValue = config.getProperty(propertyName);
    if (stringValue == null) {
      return defaultValue;
    }
    Integer integerValue = null;
    try {
      integerValue = Integer.parseInt(stringValue);
    } catch (NumberFormatException e) {
      integerValue = defaultValue;
    }
    return integerValue;
  }

  private Boolean getBooleanConfiguration(Properties config, String propertyName, Boolean defaultValue) {
    String stringValue = config.getProperty(propertyName);
    if (stringValue == null) {
      return defaultValue;
    }
    Boolean booleanValue = null;
    try {
      booleanValue = Boolean.parseBoolean(stringValue);
    } catch (NumberFormatException e) {
      booleanValue = defaultValue;
    }
    return booleanValue;
  }

  public EntityManager createEntityManager()
  {
    persistenceLog.trace(">>createEntityManager");
    EntityManager entityManager = null;
    try
    {
      persistenceLog.trace("Before createEntityManager");
      entityManager = entityManagerFactory.createEntityManager();
      persistenceLog.debug("Got entityManager " + entityManager);
      EntityTransaction tx = entityManager.getTransaction();
      persistenceLog.debug("Got transaction " + tx);
      tx.begin();
      persistenceLog.debug("Begun transaction");
    }
    catch (Exception e)
    {
      persistenceLog.error("Failed to create an EntityManager", e);
    }
    persistenceLog.trace("<<createEntityManager");
    return entityManager;
  }

  public void commitEntityManager(EntityManager entityManager)
  {
    persistenceLog.trace(">>commitEntityManager");
    if (entityManager.isOpen())
    {
      persistenceLog.debug("entityManager opened, commit transaction");
      entityManager.getTransaction().commit();
      entityManager.close();
    }
    persistenceLog.trace("<<commitEntityManager");
  }

  public void rollbackEntityManager(EntityManager entityManager)
  {
    persistenceLog.trace(">>rollbackEntityManager");
    if (entityManager.isOpen())
    {
      persistenceLog.debug("entityManager opened, rollback transaction");
      entityManager.getTransaction().rollback();
      entityManager.close();
    }
    persistenceLog.trace("<<rollbackEntityManager");
  }

}
