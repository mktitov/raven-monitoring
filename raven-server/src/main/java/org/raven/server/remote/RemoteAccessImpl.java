/*
 *  Copyright 2010 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.server.remote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import org.raven.auth.Authenticator;
import org.raven.conf.Configurator;
import org.raven.conf.impl.UserAcl;
import org.raven.remote.RemoteAccess;
import org.raven.remote.RemoteSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class RemoteAccessImpl implements RemoteAccess
{
    @Service
    private static Configurator  configurator;

    @Service
    private static Authenticator authService;

    private static Logger logger = LoggerFactory.getLogger(RemoteAccess.class);

    public RemoteSession login(String user, String password, String locale) 
            throws RemoteException
    {
        logger.info("Recieved request for login from user ({})", user);
        //add auth check here
        try {
            boolean validUser = authService.checkAuth(user, password);
            if (!validUser)
            {
                logger.warn("Authentication failed for user {}", user);
                return null;
            }
            else
            {
                logger.info("User ({}) successfully authenticated", user);
                logger.debug("Creating session for user {}", user);
                //Auth success. Creating session
                UserAcl userAcl = new UserAcl(user, configurator.getConfig());
                return (RemoteSession) UnicastRemoteObject.exportObject(
                        new RemoteSessionImpl(userAcl), 0);
            }
        } catch (Exception ex) {
            String errorMess = String.format("User (%s) authentication/authorization error", user);
            logger.error(errorMess, ex);
            throw new RemoteException(errorMess, ex);
        }
    }

}
