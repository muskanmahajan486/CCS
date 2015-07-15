/* OpenRemote, the Home of the Digital Home.
* Copyright 2008-2010, OpenRemote Inc.
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
package org.openremote.controllercommand.service.impl;

import org.apache.commons.codec.binary.Base64;
import org.openremote.controllercommand.GenericDAO;
import org.openremote.controllercommand.domain.Account;
import org.openremote.controllercommand.domain.User;
import org.openremote.controllercommand.service.AccountService;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;


/**
 * Account service implementation.
 * 
 * @author Dan Cong
 */
public class AccountServiceImpl implements AccountService {

  public static final String HTTP_AUTH_HEADER_NAME= "Authorization";
  public static final String HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX= "Basic ";
  
  protected GenericDAO genericDAO;

  public void setGenericDAO(GenericDAO genericDAO) {
     this.genericDAO = genericDAO;
  }
  
   @Transactional
   private void save(Account a) {
      genericDAO.save(a);
   }

   private User loadByUsername(String username) {
      return genericDAO.getByNonIdField(User.class, "username", username);
   }
   
   private long queryAccountIdByUsername(String username) {
      User u = genericDAO.getByNonIdField(User.class, "username", username);
      return u == null ? 0L : u.getAccount().getOid();
   }

   private boolean isHTTPBasicAuthorized(long accountId, String credentials, boolean isPasswordEncoded) {
      if (credentials != null && credentials.startsWith(HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX)) {
         credentials = credentials.replaceAll(HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX, "");
         credentials = new String(Base64.decodeBase64(credentials.getBytes()));
         String[] arr = credentials.split(":");
         if (arr.length == 2) {
            String username = arr[0];
            String password = arr[1];
            long accId = queryAccountIdByUsername(username);
            if (accId == 0L || accId != accountId) {
               return false;
            }
            User user = loadByUsername(username);
            if (!isPasswordEncoded) {
               password = new Md5PasswordEncoder().encodePassword(password, username);
            }
            if (user != null && user.getPassword().equals(password)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isHTTPBasicAuthorized(long accountId, String credentials) {
      return isHTTPBasicAuthorized(accountId, credentials, true);
   }

   private boolean isHTTPBasicAuthorized(String username, String credentials, boolean isPasswordEncoded) {
      return isHTTPBasicAuthorized(queryAccountIdByUsername(username), credentials, isPasswordEncoded);
   }

   @Override
   public User loadByHTTPBasicCredentials(String credentials) {
      if (credentials.startsWith(HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX)) {
         credentials = credentials.replaceAll(HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX, "");
         credentials = new String(Base64.decodeBase64(credentials.getBytes()));
         String[] arr = credentials.split(":");
         if (arr.length == 2) {
            String username = arr[0];
            String password = arr[1];
            User user = loadByUsername(username);
            String encodedPassword = new Md5PasswordEncoder().encodePassword(password, username);
            if (user != null && user.getPassword().equals(encodedPassword)) {
               return user;
            }
         }
      }
      // let's be lax and not throw a BAD_REQUEST to allow the user to retry
      return null;
   }
}
