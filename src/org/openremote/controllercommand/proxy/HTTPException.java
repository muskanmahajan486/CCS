package org.openremote.controllercommand.proxy;

@SuppressWarnings("serial")
public class HTTPException extends Exception {

   private int status;
   private boolean json;
   private boolean options;

   public HTTPException(int status, boolean json, boolean options) {
      super();
      this.status = status;
      this.json = json;
      this.options = options;
   }

   public int getStatus() {
      return status;
   }
   
   public boolean isJson() {
     return json;
   }

   public boolean isOptionsRequest() {
     return options;
   }
}
