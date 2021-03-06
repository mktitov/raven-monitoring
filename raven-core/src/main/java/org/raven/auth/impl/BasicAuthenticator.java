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

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.AuthenticatorException;
import org.raven.auth.UserContextConfig;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=AuthenticatorsNode.class, importChildTypesFrom=IpFiltersNode.class)
public class BasicAuthenticator extends AbstractAuthenticatorNode {
    @NotNull @Parameter
    private String password;

    @Override
    protected boolean doCheckAuth(UserContextConfig user, String password) throws AuthenticatorException {
        return isStarted() && user.getLogin().equals(getName()) && password.equals(this.password);
    }
    
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
