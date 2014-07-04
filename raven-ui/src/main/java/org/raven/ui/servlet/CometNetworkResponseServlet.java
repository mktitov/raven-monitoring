/*
 * Copyright 2014 Mikhail Titov.
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

package org.raven.ui.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.CometEvent;
import org.apache.catalina.CometProcessor;
import org.apache.tapestry5.ioc.Registry;
import org.raven.net.NetworkResponseService;
import org.raven.net.ResponseContext;
import org.raven.ui.util.RavenRegistry;

/**
 *
 * @author Mikhail Titov
 */
public class CometNetworkResponseServlet extends NetworkResponseServlet implements CometProcessor {

    public void event(CometEvent ce) throws IOException, ServletException {
        final HttpServletRequest request = ce.getHttpServletRequest();
        final HttpServletResponse response = ce.getHttpServletResponse();
        switch (ce.getEventType()) {
            case BEGIN:
                initResponseProcessing(ce, request, response);
                break;
            case READ:
                break;
            case END:
                break;
            case ERROR:
                break;
        }
    }
    
    private void initResponseProcessing(CometEvent ce, HttpServletRequest request, HttpServletResponse response) {
        Registry registry = RavenRegistry.getRegistry();
        NetworkResponseService responseService = registry.getService(NetworkResponseService.class);
        ResponseContext responseContext = null;
    }
    
}
