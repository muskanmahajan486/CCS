package org.openremote.controllercommand.domain;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name="initiate_proxy_controller_command")
public class InitiateProxyControllerCommand extends ControllerCommand {
   private String url;
   private String token;

   public InitiateProxyControllerCommand() {
      super();
   }

   public InitiateProxyControllerCommand(Account account, ControllerCommandDTO.Type type, String url) {
      super(account, type);
      this.url = url;
      this.token = UUID.randomUUID().toString();
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public String getUrl() {
      return url;
   }

   public void setToken(String token) {
      this.token = token;
   }

   public String getToken() {
      return token;
   }
}
