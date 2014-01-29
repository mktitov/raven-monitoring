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
package org.raven.net;

import java.util.Map;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.slf4j.Logger;

/**
 * Response context for SRI (simple response interface)
 * @author Mikhail Titov
 */
public interface ResponseContext {
    /**
     * Returns the login service
     */
    public LoginService getLoginService();
    /**
     * Returns requests for which this response is generating
     */
    public Request getRequest();
    /**
     * Returns the map that can be used to add response headers (not thread safe!)
     */
    public Map<String, String> getHeaders();
    /**
     * Returns the access rights to response builder node for user passed in the parameter
     */
//    public int getAccessRightsForUser(UserContext user);
    /**
     * Returns true if access is granted for given user to 
     * @param user
     * @return 
     */
    public boolean isAccessGranted(UserContext user);
    /**
     * Returns <b>true</b> if session creation allowed
     */
    public boolean isSessionAllowed();
    /**
     * Generate and return the response for given user and request parameters
     * @throws NetworkResponseServiceExeption 
     */
    public Response getResponse(UserContext user) throws NetworkResponseServiceExeption;
    /**
     * Returns the response builder. To build response use {@link #getResponse(org.raven.auth.UserContext) }, 
     * don't use the result of this method to build response
     */
    public ResponseBuilder getResponseBuilder();
    /**
     * Returns the response service logger
     */
    public Logger getLogger();
    /**
     * Returns the response builder logger
     */
    public Logger getResponseBuilderLogger();
//    public String getPath();
//    public String getBuilderPath();
//    public String getSubpath();
}
