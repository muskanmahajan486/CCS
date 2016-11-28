package org.openremote.controllercommand;

import java.io.IOException;

public class WSException extends Exception {
   public WSException(IOException cause) {
      super(cause);
   }

   public WSException(String message) {
      super(message);
   }
}
