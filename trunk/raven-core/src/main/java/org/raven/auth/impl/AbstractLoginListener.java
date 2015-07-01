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

import org.raven.auth.LoginListener;
import org.raven.auth.UserContext;
import org.raven.tree.impl.BaseNodeWithStat;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractLoginListener extends BaseNodeWithStat implements LoginListener {

    public void userLoggedIn(UserContext userContext) {
        long ts = stat.markOperationProcessingStart();
        try {
            onUserLoggedIn(userContext);
        } finally {
            stat.markOperationProcessingEnd(ts);
        }
    }
    
    protected abstract void onUserLoggedIn(UserContext userContext);
}
