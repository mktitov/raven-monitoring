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

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
@Ignore
public class HttpServerTest implements HttpHandler
{
    @Test
    public void test() throws IOException
    {
        InetSocketAddress addr = new InetSocketAddress(9999);
        HttpServer server = HttpServer.create(addr, 0);
        try
        {
            HttpContext context = server.createContext("/test", this);
            context.getFilters().add(new SimpleFilter());
            context.setAuthenticator(new BasicAuthenticator("test") {

                @Override
                public boolean checkCredentials(String arg0, String arg1) {
                    return true;
                }
            });
            server.setExecutor(null);
            server.start();

            System.in.read();
        }
        finally
        {
            server.stop(0);
        }
    }

    public void handle(HttpExchange event) throws IOException
    {
        Headers headers = event.getRequestHeaders();
//        headers.
//        event.sendResponseHeaders(401, 0);
//        event.close();
        byte[] hello = "Hello world".getBytes();
        event.sendResponseHeaders(200, hello.length);
        OutputStream out = event.getResponseBody();
        out.write(hello);
        out.close();
    }

    private class SimpleFilter extends Filter
    {
        @Override
        public void doFilter(HttpExchange event, Chain chain) throws IOException {
            System.out.println("Filtered");
        }

        @Override
        public String description() {
            return "simple filter";
        }
    }
}
