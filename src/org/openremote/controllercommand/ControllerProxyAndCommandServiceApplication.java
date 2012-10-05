package org.openremote.controllercommand;

import org.openremote.controllercommand.proxy.ProxyServer;
import org.openremote.controllercommand.service.AccountService;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.restlet.Application;

public class ControllerProxyAndCommandServiceApplication extends Application
{

  public ControllerProxyAndCommandServiceApplication(String proxyHostname, Integer proxyTimeout, Integer proxyPort, 
          String proxyClientPortRange, ControllerCommandService controllerCommandService, AccountService accountService)
  {
    ProxyServer ps = new ProxyServer(proxyHostname, proxyTimeout, proxyPort, proxyClientPortRange, controllerCommandService, accountService);
    ps.start();
  }


}
