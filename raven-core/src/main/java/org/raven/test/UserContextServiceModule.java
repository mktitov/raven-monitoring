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

package org.raven.test;

import org.raven.auth.UserContext;
import org.raven.auth.UserContextService;

/**
 *
 * @author Mikhail Titov
 */
public class UserContextServiceModule implements UserContextService
{
    private static UserContext context = null;

    public static UserContextService buildUserContextService()
    {
        return new UserContextServiceModule();
    }

    public UserContext getUserContext()
    {
        return context;
    }

    public static void setUserContext(UserContext userContext)
    {
        context = userContext;
    }
}
