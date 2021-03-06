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

package org.raven.auth.impl;

import java.util.Collection;
import org.raven.auth.AuthProvider;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.conf.Configurator;
import org.raven.net.ResponseContext;
import org.raven.net.http.server.SessionManager;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
@Deprecated
public class AuthServiceImpl implements LoginService
{
    private final static String ROOT_USER_NAME = "root";

    private final Logger logger;
    private final Collection<AuthProvider> authProviders;
    private final Configurator configurator;

    public AuthServiceImpl(Collection<AuthProvider> authProviders, Configurator configurator, Logger logger)
    {
        this.authProviders = authProviders;
        this.configurator = configurator;
        this.logger = logger;
    }

    public boolean isLoginAllowedFromIp(String ip) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public UserContext login(String username, String password, String host, ResponseContext responseContext)
    {
        if (logger.isInfoEnabled())
            logger.info("Authenticating user ({})", username);
        if (ROOT_USER_NAME.equals(username)){
            try {
                String rootPass = configurator.getConfig().getStringProperty(Configurator.AUTH_ROOT_PASSWORD, null);
                if (rootPass==null){
                    if (logger.isWarnEnabled())
                        logger.warn("Can't authenticate root user because of root password "
                                + "not defined in the configuration");
                    return null;
                }
                if (!rootPass.equals(password))
                    return null;
                else {
//                    UserContext context = new UserContextImpl(username, "root");
//                    context.setAdmin(true);
//                    return context;
                    return null;
                }
            } catch (Exception ex) {
                if (logger.isErrorEnabled())
                    logger.error("Error extracting root password from configuration", ex);
                return null;
            }
        }
        for (AuthProvider provider: authProviders) {
            String providerName = provider.authenticate(username, password);
            if (providerName!=null)
            {
                if (logger.isInfoEnabled())
                    logger.info("User ({}) was successefully authenticated by provider ({})",
                            username, providerName);
//                return new UserContextImpl(username, providerName);
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean isStarted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SessionManager getSessionManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
