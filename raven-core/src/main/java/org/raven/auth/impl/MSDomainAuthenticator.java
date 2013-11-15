/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.auth.impl;

import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbSession;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.AuthenticatorException;
import org.raven.auth.UserContextConfig;
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=AuthenticatorsNode.class, importChildTypesFrom=IpFiltersNode.class)
public class MSDomainAuthenticator extends AbstractAuthenticatorNode {
    
    public final static String DOMAIN_PARAM = "domain";
    
    @NotNull @Parameter
    private String domainController;
    @NotNull @Parameter
    private String defaultDomain;

    @Override
    protected boolean doCheckAuth(UserContextConfig userCtx, String password) throws AuthenticatorException {
        if (!isStarted()) 
            return false;        
        try {
            String[] elems = RavenUtils.split(userCtx.getLogin(), "\\");
            String user = elems[elems.length-1];
            String domain = elems.length==2? elems[0] : defaultDomain;
            String[] controllers = domainController.split("\\s*,\\s*");
            int ind = -1;
            while (++ind < controllers.length)
                try {
                    boolean res = checkAuthOnController(controllers[ind], domain, user, password, userCtx);
                    if (ind>0) reorderControllers(controllers, ind);
                    return res;
                } catch (Throwable e) {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error(String.format("Authentication error on MS domain controller (%s)", controllers[ind]), e);
                    if (ind==controllers.length-1)
                        throw new AuthenticatorException(
                            String.format("Authentication error on MS domain controller (%s)", controllers[ind]), e);
                }
            return false;
        } catch (Throwable e) {
            if (e instanceof AuthenticatorException) throw (AuthenticatorException)e;
            else throw new AuthenticatorException("MS Domain authentication error.", e);
        }
    }

    public String getDomainController() {
        return domainController;
    }

    public void setDomainController(String domainController) {
        this.domainController = domainController;
    }

    public String getDefaultDomain() {
        return defaultDomain;
    }

    public void setDefaultDomain(String defaultDomain) {
        this.defaultDomain = defaultDomain;
    }

    private boolean checkAuthOnController(String controller, String domain, String user, String password, UserContextConfig userCtx) 
            throws Exception 
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug(String.format("Trying to authenticate user (%s) at domain (%s) "
                    + "using domain controller (%s)", user, domain, controller));
        NtlmPasswordAuthentication ntlm = new NtlmPasswordAuthentication(domain, user, password);
        UniAddress controllerAddr = UniAddress.getByName(controller, true);
        try {
            SmbSession.logon(controllerAddr, ntlm);
            userCtx.setLogin(user);
            userCtx.getParams().put(DOMAIN_PARAM, domain);
            return true;
        } catch (SmbAuthException ae) {
            if (isLogLevelEnabled(LogLevel.WARN)) 
                getLogger().warn(String.format("Authentication failed for user (%s) at domain (%s) using "
                    + "domain controller (%s). %s", user, domain, controller, ae.getMessage()));
            return false;
        }
    }

    private void reorderControllers(String[] controllers, int ind) {
        StringBuilder buf = new StringBuilder(controllers[ind]);
        for (int i=0; i<controllers.length; ++i)
            if (ind!=i) buf.append(", ").append(controllers[i]);        
        domainController = buf.toString();
        if (isLogLevelEnabled(LogLevel.WARN))
            getLogger().warn("Domain controllers order were reordered. New order is ({})", domainController);
    }
}
