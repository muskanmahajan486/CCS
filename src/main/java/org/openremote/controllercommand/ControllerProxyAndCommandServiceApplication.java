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
package org.openremote.controllercommand;

import org.glassfish.jersey.server.ResourceConfig;
import org.openremote.beehive.EntityTransactionFilter;
import org.openremote.controllercommand.proxy.ProxyServer;
import org.openremote.controllercommand.resources.ControllerCommandResource;
import org.openremote.controllercommand.resources.ControllerCommandsResource;
import org.openremote.controllercommand.resources.ControllerSessionHandler;
import org.openremote.controllercommand.resources.ControllersResource;
import org.openremote.controllercommand.service.AccountService;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.util.Properties;

public class ControllerProxyAndCommandServiceApplication extends ResourceConfig
{
  static private EntityManagerFactory entityManagerFactory;

  protected final static Logger persistenceLog = LoggerFactory.getLogger(ControllerProxyAndCommandServiceApplication.class);
  private final ControllerSessionHandler controllerSessionHandler;

  public ControllerProxyAndCommandServiceApplication()
  {
    Properties config = new Properties();
    try
    {
      config.load(getClassLoader().getResourceAsStream("config.properties"));
    } catch (IOException e)
    {
      persistenceLog.error("Error loading config",e);
    }

    entityManagerFactory = Persistence.createEntityManagerFactory(config.getProperty("persistenceUnitName", "CCS-MySQL"));
    persistenceLog.info("EntityManagerFactory has been created");
    register(EntityPersistence.class);

    GenericDAO genericDAO = new GenericDAO();
    final AccountService accountService = new AccountService();
    accountService.setGenericDAO(genericDAO);
    final ControllerCommandService controllerCommandService = new ControllerCommandService();
    controllerCommandService.setGenericDAO(genericDAO);

    controllerSessionHandler = ControllerSessionHandler.getInstance();
    controllerSessionHandler.setEntityFilter(this);

    controllerSessionHandler.setControllerCommandService(controllerCommandService);
    controllerSessionHandler.setAccountService(accountService);
    String baseUri = config.getProperty("wsHook.baseUri", "localhost:3000");
    String openPath = config.getProperty("wsHook.openPath", "/open");
    String closePath = config.getProperty("wsHook.closePath", "/close");
    String ip = config.getProperty("ccs.ip", "127.0.0.1");
    long retryTimeout = getIntegerConfiguration(config,"wsHook.retryTimeout", 30000);

    controllerSessionHandler.prepareConnectionNotification(baseUri, openPath, closePath, ip, retryTimeout);

    String proxyHostname = config.getProperty("proxy.hostname", "localhost");
    Integer proxyTimeout = getIntegerConfiguration(config, "proxy.timeout", 10000);
    Integer proxyPort = getIntegerConfiguration(config, "proxy.port", 10000);
    String proxyClientPortRange = config.getProperty("proxy.clientPortRange", "30000-30010");
    Boolean useSSL = getBooleanConfiguration(config, "proxy.useSSL", true);
    String keystore = config.getProperty("proxy.keystore", "keystore.ks");
    String keystorePassword = config.getProperty("proxy.keystorePassword", "storepass");
    Long commandRetryTimeout = getLongConfiguration(config, "command.retry.timeout", 30000l);
    Long commandLiveTimeout = getLongConfiguration(config, "command.live.timeout", 43200l);

    //retrieve failled command thread
    CommandRetrieverThread cr = new CommandRetrieverThread(this, controllerCommandService, controllerSessionHandler,commandRetryTimeout,commandLiveTimeout);
    cr.start();

    ProxyServer ps = new ProxyServer(proxyHostname, proxyTimeout, proxyPort, proxyClientPortRange, useSSL, keystore, keystorePassword, controllerCommandService, accountService, this, controllerSessionHandler);
    ps.start();
    register(ControllerCommandResource.class);

    register(ControllerCommandsResource.class);
    register(ControllersResource.class);


    register(new org.glassfish.hk2.utilities.binding.AbstractBinder()
             {
               @Override
               protected void configure()
               {
                 bind(accountService);
                 bind(controllerCommandService);
                 bind(controllerSessionHandler);
               }
             });
  }


    @PreDestroy
  private void testDestroy() {
    try {
      controllerSessionHandler.shutdown();
      entityManagerFactory.close();
      persistenceLog.info("EntityManagerFactory has been closed");
    } catch (Exception e) {
      persistenceLog.warn("Could not close EntityManagerFactory");
    }
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

  private Long getLongConfiguration(Properties config, String propertyName, Long defaultValue) {
    String stringValue = config.getProperty(propertyName);
    if (stringValue == null) {
      return defaultValue;
    }
    Long longValue = null;
    try {
      longValue = Long.parseLong(stringValue);
    } catch (NumberFormatException e) {
      longValue = defaultValue;
    }
    return longValue;
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

  /**
   * This container filter implements a managed persistence context for JPA entities used
   * in JAX-RS resources.
   */
  private static class EntityPersistence implements ContainerRequestFilter, ContainerResponseFilter
  {
    // Class Members ------------------------------------------------------------------------------

    // ContainerRequestFilter Implementation ------------------------------------------------------

    /**
     * Passes the entity manager reference as a request property to the resource classes to use.
     * Also begins the transaction boundary for JPA entities here.
     */
    @Override public void filter(ContainerRequestContext request)
    {
      EntityManager entityManager = null;
      try
      {
        entityManager = entityManagerFactory.createEntityManager();
        request.setProperty(EntityTransactionFilter.PERSISTENCE_ENTITY_MANAGER_LOOKUP, entityManager);
        entityManager.getTransaction().begin();
      }

      catch (Throwable throwable)
      {

        persistenceLog.error("Failed to create EntityManager", throwable);

        if (entityManager != null)
        {
          EntityTransaction tx = entityManager.getTransaction();
          if (tx != null && tx.isActive())
          {
            try
            {
              tx.rollback();
            } catch (Exception e) {
              persistenceLog.warn("Failed to rollback transaction ", e);
            }
          }
        }
      }
    }

    /**
     * Manages the entity transaction boundary on return request. If entity transaction has
     * been marked for rollback, or we are returning an HTTP error code 400 or above, roll back
     * the entity transaction.
     */
    @Override public void filter(ContainerRequestContext request, ContainerResponseContext response)
    {
      EntityManager entityManager = (EntityManager) request.getProperty(EntityTransactionFilter.PERSISTENCE_ENTITY_MANAGER_LOOKUP);
      if (entityManager == null)
      {
        return;
      }

      EntityTransaction tx = entityManager.getTransaction();

      persistenceLog.debug("Transaction is " + tx + " , active ? " + (tx.isActive()?"yes":"no"));

      if (tx != null && tx.isActive())
      {
        if (tx.getRollbackOnly() || response.getStatus() >= 400)
        {
          persistenceLog.debug("Rolling back transaction");
          try
          {
            tx.rollback();
          } catch (Exception e) {
            persistenceLog.warn("Failed to rollback transaction ", e);
          }
        }
        else
        {
          persistenceLog.debug("Commit transaction");
          try
          {
            tx.commit();
          } catch (Exception e) {
            persistenceLog.error("Failed to commit transaction ", e);
          }
        }
      }
      if (entityManager.isOpen())
      {
        try
        {
          entityManager.close();
        } catch (Exception e) {
          persistenceLog.warn("Failed to closed EntityManager", e);
        }
      }
    }
  }

}
