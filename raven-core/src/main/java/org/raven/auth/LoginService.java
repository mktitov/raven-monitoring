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

package org.raven.auth;

import org.raven.net.ResponseContext;
import org.raven.net.http.server.HttpSession;

/**
 *
 * @author Mikhail Titov
 */
public interface LoginService
{
    public static final String ROOT_USER_NAME = "root";
    public static final String PUBLIC_GROUP = "PUBLIC";
    
    public HttpSession getSession(String sessionId);
    public HttpSession createSession();
//    public UserContext login(String username, String password, String ip, boolean needServerSession) 
    public UserContext login(String username, String password, String ip, ResponseContext responseContext) throws LoginException;
//    public UserContext login(String username, String password, String ip) throws LoginException;
//    public UserContext getUserForSessionId(String sessionId);
    public boolean isLoginAllowedFromIp(String ip);
    public int getId();
    public String getName();
}
