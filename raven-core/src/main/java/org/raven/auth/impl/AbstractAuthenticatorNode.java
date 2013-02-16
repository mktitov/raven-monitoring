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

import org.raven.auth.Authenticator;
import org.raven.auth.AuthenticatorException;
import org.raven.tree.impl.BaseNodeWithStat;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractAuthenticatorNode extends BaseNodeWithStat implements Authenticator {

    public AbstractAuthenticatorNode() {
    }

    public AbstractAuthenticatorNode(String name) {
        super(name);
    }

    public boolean checkAuth(String login, String password) throws AuthenticatorException {
        long ts = stat.markOperationProcessingStart();
        try {
            return doCheckAuth(login, password);
        } finally {
            stat.markOperationProcessingEnd(ts);
        }
    }
    
    protected abstract boolean doCheckAuth(String login, String password) throws AuthenticatorException;
}
