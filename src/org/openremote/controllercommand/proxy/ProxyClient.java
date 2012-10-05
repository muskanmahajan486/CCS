package org.openremote.controllercommand.proxy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.openremote.controllercommand.domain.InitiateProxyControllerCommand;
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.service.AccountService;
import org.openremote.controllercommand.service.ControllerCommandService;

public class ProxyClient extends Proxy {

   private static Logger logger = Logger.getLogger(ProxyClient.class);
   private ProxyServer server;
   private String hostName;
   private int minClientPort;
   private int maxClientPort;
   private AccountService accountService;
   private ControllerCommandService controllerCommandService;

   public ProxyClient(ProxyServer server, SocketChannel clientSocket, int timeout, String hostName, int minClientPort, int maxClientPort,
           ControllerCommandService controllerCommandService, AccountService accountService)  throws IOException {
      super(clientSocket, timeout);
      this.server = server;
      this.hostName = hostName;
      this.minClientPort = minClientPort;
      this.maxClientPort = maxClientPort;
      this.accountService = accountService;
      this.controllerCommandService = controllerCommandService;
   }

   protected void onProxyExit() {
      server.unregister(this);
   }

   protected SocketChannel openDestinationSocket() throws IOException {
      // this either returns a good user, or throws
      User user = authenticateUser();
      ServerSocketChannel serverSocket = ServerSocketChannel.open();
      int localPort;
      try{
         serverSocket.configureBlocking(false);
         logger.info("Binding socket for client");
         for (localPort = minClientPort; localPort <= maxClientPort; localPort++)
         {
             try
             {
                 serverSocket.socket().bind(new InetSocketAddress(localPort));
                 if (serverSocket.socket().getLocalPort() != -1) {
                     break;
                 }
             }
             catch (IOException ignoreAndContinue){}
         }
         
         localPort = serverSocket.socket().getLocalPort();
         if (localPort == -1) {
             throw new IOException("Could not bind local socket between ports " + minClientPort + " and " + maxClientPort);
         }
         logger.info("Socket bound to port "+localPort);

         // now let's tell the client we want connection here
         InitiateProxyControllerCommand controllerCommand = contactController(user, localPort);
         try{
            // we start with accepting the request
            SelectionKey serverKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            // now wait for the client to connect
            while(selector.select(timeout) > 0){
               logger.info("Out of select");
               if(halted){
                  break;
               }
               selector.selectedKeys().clear();
               if(serverKey.isAcceptable()){
                  SocketChannel clientSocket = serverSocket.accept();
                  // this can happen if the TCP layer had data but its checksum was invalid
                  if(clientSocket == null){
                     logger.info("Accepted null, looping");
                     continue;
                  }
                  // we have a socket, let's cancel the key and return the client socket
                  logger.info("We have a client, now let's check the token");
                  serverKey.cancel();
                  // this throws on error 
                  checkToken(clientSocket, controllerCommand);
                  return clientSocket;
               }
            }
            // we timed out
         }finally{
            // we got contacted, or not but let's drop this command since we're not listening anymore
            ControllerCommandService controllerCommandService = getControllerCommandService();
            controllerCommandService.closeControllerCommand(controllerCommand);
            controllerCommandService.update(controllerCommand);
         }
      }finally{
         try{
            serverSocket.close();
         }catch(IOException x){
            // ignore in finally
         }

      }
      logger.info("Halted");
      throw new IOException("We've been halted");
   }

   private void checkToken(SocketChannel clientSocket,
         InitiateProxyControllerCommand controllerCommand) throws IOException {
      try{
         byte[] token = controllerCommand.getToken().getBytes("ASCII");
         ByteBuffer buffer = ByteBuffer.allocate(token.length);
         clientSocket.configureBlocking(false);
         SelectionKey clientKey = clientSocket.register(selector, SelectionKey.OP_READ);
         while(selector.select(timeout) > 0){
            logger.info("Out of select");
            if(halted){
               break;
            }
            selector.selectedKeys().clear();
            if(clientKey.isReadable()){
               logger.info("Client is readable");
               clientSocket.read(buffer);
               if(buffer.hasRemaining()){
                  logger.info("Still things to read, let's loop");
                  continue;
               }
               logger.info("Checking token");
               // we have read it all, now compare
               byte[] readToken = buffer.array();
               for(int i=0;i<token.length;i++)
                  if(token[i] != readToken[i])
                     throw new IOException("Client connected with invalid token");
               // all good!
               logger.info("Token is good");
               return;
            }
            logger.info("Back in select");
         }
         // we timed out or were halted
         throw new IOException("Timed out or halted before we could read the token");
      }catch(IOException x){
         clientSocket.close();
         throw x;
      }
   }

   private User authenticateUser() throws IOException {
      // we need to read data from the client until we get the Authentication header
      SelectionKey key = srcSocket.register(selector, SelectionKey.OP_READ);
      try{
         logger.info("Selecting for headers");
         while(selector.select(timeout) > 0){
            logger.info("out of select");
            if(halted)
               break;
            selector.selectedKeys().clear();
            if(key.isReadable()){
               int read = srcSocket.read(srcBuffer);
               if(read == -1){
                  // we've reached EOF, drop this client
                  throw new HTTPException(HttpURLConnection.HTTP_UNAUTHORIZED);
               }
               // did we read anything new?
               if(read == 0)
                  continue;
               // we have new data, let's look at it
               User user = getAuthenticatedUser(srcBuffer);
               if(user != null)
                  return user;
               // we don't have enough headers yet, but we must give up if we don't have room anymore in the buffer
               if(!srcBuffer.hasRemaining())
                  throw new HTTPException(HttpURLConnection.HTTP_BAD_REQUEST);
            }
         }
         // for a timeout or were halted we just close the connection upstream by throwing
         throw new IOException("Connection timed-out before we could read the authentication header");
      }catch(HTTPException x){
         // we must reply with an error
         throw sendError(srcSocket, x.getStatus());
      }
   }

   private IOException sendError(SocketChannel srcSocket, int status) throws IOException {
      // Construct the message
      String reason;
      String maybeHeader = "";
      switch(status){
      case HttpURLConnection.HTTP_BAD_REQUEST:
         reason = "Bad request";
         break;
      case HttpURLConnection.HTTP_UNAUTHORIZED:
         reason = "Unauthorized";
         maybeHeader = "WWW-Authenticate: Basic realm=\"OPENREMOTE_Beehive\"\r\n";
         break;
      case HttpURLConnection.HTTP_FORBIDDEN:
         reason = "Forbidden";
         break;
      default:
         reason = "Unknown error";
      }
      String response = "HTTP/1.1 "+status+" "+reason+"\r\n" + 
         maybeHeader +
         "Content-Length: 0\r\n" +
         "\r\n";
      srcBuffer.clear();
      srcBuffer.put(response.getBytes("ASCII"));
      srcBuffer.flip();
      // now send it
      SelectionKey key = srcSocket.register(selector, SelectionKey.OP_WRITE);
      logger.info("Selecting for sending error "+response);
      while(selector.select(timeout) > 0){
         logger.info("Out of select");
         if(halted)
            break;
         selector.selectedKeys().clear();
         if(key.isWritable()){
            logger.info("Can write");
            srcSocket.write(srcBuffer);
            if(srcBuffer.hasRemaining()){
               logger.info("Some left to write");
               continue;
            }
            // we're done, let bail
            return new IOException("Error message (most likely wrong authentication data) was sent to client.");
         }
         logger.info("Back in select");
      }
      // make sure we unwind until we close this socket and drop the ball
      return new IOException("Failed to send error message to client");
   }

   private User getAuthenticatedUser(ByteBuffer srcBuffer) throws HTTPException {
      // http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4 says we should look for \r\nAuthorization:(.*)\r\n([^ \t]) as long
      // as it's not preceded by "\r\n\r\n" which indicates the end of the header section
      // we cannot parse data after the headers, but the headers are always in ASCII, so find that limit first
      byte[] bytes = srcBuffer.array();
      int limit = srcBuffer.position();
      int headerEnd = limit;
      for(int i=0;i<limit-3;i++){
         if(bytes[i] == '\r'
            && bytes[i+1] == '\n'
               && bytes[i+2] == '\r'
                  && bytes[i+3] == '\n'){
            headerEnd = i+4;
            break;
         }
      }
      // in any case, non-ASCII is after the end of headers, or since we haven't reached that yet, after the whole limit
      // so we can parse as ASCII characters
      String headers = new String(bytes, 0, headerEnd, Charset.forName("ASCII"));
      // so do we have an Authentication header in there?
      Pattern pattern = Pattern.compile("\r\nAuthorization:([^\r\n]*)\r\n([^ \t])", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(headers);
      // nothing?
      if(!matcher.find()){
         if(headerEnd <= limit){
            // we have seen every header, and found no good one, let's quit
            throw new HTTPException(HttpURLConnection.HTTP_UNAUTHORIZED);
         }
         // we haven't seen the end of headers yet, don't give up and read more
         return null;
      }
      // we have a value!
      String value = matcher.group(1);
      // now fold it
      value = value.replaceAll("\r\n[ \t]+", " ").trim();
      // and attempt to validate it
      User user = getAccountService().loadByHTTPBasicCredentials(value);
      if(user == null){
         // authentication failed
         throw new HTTPException(HttpURLConnection.HTTP_UNAUTHORIZED);
      }
      return user;
   }

   protected ControllerCommandService getControllerCommandService() {
      return this.controllerCommandService;
   }

   protected AccountService getAccountService() {
      return this.accountService;
   }

   private InitiateProxyControllerCommand contactController(User user, int port) {
      ControllerCommandService controllerCommandService = getControllerCommandService();
      return controllerCommandService.saveProxyControllerCommand(user, "http://"+hostName+":"+port);
   }

}
