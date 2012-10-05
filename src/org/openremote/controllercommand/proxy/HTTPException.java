package org.openremote.controllercommand.proxy;

@SuppressWarnings("serial")
public class HTTPException extends Exception {

   private int status;

   public HTTPException(int status) {
      super();
      this.status = status;
   }

   public int getStatus() {
      return status;
   }

}
