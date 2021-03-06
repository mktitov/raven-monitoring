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

package org.raven.server.app.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.raven.rest.beans.NodeBean;

/**
 *
 * @author Mikhail Titov
 */
@Path("/helloworld/")
public class HellowWorldResource
{
    @GET
    @Produces("text/plain")
    public String getGreeting(@QueryParam("path") String path)
    {
        return "Hello world. Body: "+path;
    }

    @Path("/testJson/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public NodeBean getJson()
    {
        return new NodeBean(1, "node name", "path to node", "class", "path to icon", true, 0, 0);
    }
}
