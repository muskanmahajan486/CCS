package org.openremote.controllercommand;

import org.glassfish.jersey.server.ResourceConfig;
import org.openremote.beehive.EntityTransactionFilter;
import org.openremote.controllercommand.proxy.ProxyServer;
import org.openremote.controllercommand.resources.ControllerCommandResource;
import org.openremote.controllercommand.resources.ControllerCommandsResource;
import org.openremote.controllercommand.service.impl.AccountServiceImpl;
import org.openremote.controllercommand.service.impl.ControllerCommandServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public ControllerProxyAndCommandServiceApplication()
  {
    Properties config = new Properties();
    try
    {
      config.load(getClassLoader().getResourceAsStream("config.properties"));
    } catch (IOException e)
    {
      // TODO: log
      e.printStackTrace();
    }

    entityManagerFactory = Persistence.createEntityManagerFactory(config.getProperty("persistenceUnitName", "CCS-MySQL"));
    register(EntityPersistence.class);

    GenericDAO genericDAO = new GenericDAO();
    final AccountServiceImpl accountService = new AccountServiceImpl();
    accountService.setGenericDAO(genericDAO);
    final ControllerCommandServiceImpl controllerCommandService = new ControllerCommandServiceImpl();
    controllerCommandService.setGenericDAO(genericDAO);


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
