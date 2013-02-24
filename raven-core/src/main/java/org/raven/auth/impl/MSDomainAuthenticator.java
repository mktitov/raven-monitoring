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
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=AuthenticatorsNode.class)
public class MSDomainAuthenticator extends AbstractAuthenticatorNode {
    @NotNull @Parameter
    private String domainController;
    @NotNull @Parameter
    private String defaultDomain;

    public boolean doCheckAuth(String login, String password, String ip) throws AuthenticatorException {
        if (!isStarted()) 
            return false;
        try {
            String[] elems = RavenUtils.split(login, "\\");
            String user = elems[elems.length-1];
            String domain = elems.length==2? elems[0] : defaultDomain;
            String controller = domainController;
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug(String.format("Trying to authenticate user (%s) at domain (%s) "
                        + "using domain controller (%s)", user, domain, controller));
            NtlmPasswordAuthentication ntlm = new NtlmPasswordAuthentication(domain, user, password);
            UniAddress controllerAddr = UniAddress.getByName(controller, true);
            try {
                SmbSession.logon(controllerAddr, ntlm);
                return true;
            } catch (SmbAuthException ae) {
                if (isLogLevelEnabled(LogLevel.WARN)) 
                    getLogger().warn(String.format("Authentication failed for user (%s) at domain (%s) using "
                        + "domain controller (%s). %s", user, domain, controller, ae.getMessage()));
                return false;
            }
        } catch (Throwable e) {
            throw new AuthenticatorException("MS Domain authentication error", e);
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
}
