/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
@Ignore
public class JettyTest
{
    @Test
    public void test() throws Exception
    {
        Server server = new Server(9999);
        Handler handler = new Handler();
        server.setHandler(handler);
        server.start();

        System.in.read();
    }

    private class Handler extends AbstractHandler
    {
        public void handle(
                String target, Request request
                , HttpServletRequest servletRequest, HttpServletResponse response)
                throws IOException, ServletException
        {
            String requestAuth = request.getHeader("Authorization");
            if (requestAuth==null)
            {
                response.setHeader(
                        "WWW-Authenticate", "BASIC realm=\"RAVEN simple request interface\"");
                response.sendError(response.SC_UNAUTHORIZED);
                request.setHandled(true);
            }
            else
            {
                if (request.getMethod().equals("PUT"))
                {
                    InputStream is = servletRequest.getInputStream();
                    Collection<String> lines = IOUtils.readLines(is);
                    FileUtils.writeLines(new File("target/put.log"), lines);
                    is.close();
                }
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("<h1>Hello</h1>");
                request.setHandled(true);
            }
        }
    }
}
