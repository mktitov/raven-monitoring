/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.valve;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.CometEvent;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

/**
 *
 * @author Mikhail Titov
 */
public class CometSupportValve implements Valve {
    private Valve next;

    @Override
    public String getInfo() {
        return "Raven comet support valve";
    }

    @Override
    public Valve getNext() {
        return next;
    }

    @Override
    public void setNext(Valve valve) {
        this.next = valve;
    }

    @Override
    public void backgroundProcess() { }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
//        request.getContext().getLogger().info("ADDED COMET SUPPORT");
        request.setAttribute("connectorRequest", request);
        next.invoke(request, response);
    }

    @Override
    public void event(Request request, Response response, CometEvent event) throws IOException, ServletException {
//        if (event.getEventType()==CometEvent.EventType.BEGIN) {
//            request.getContext().getLogger().info("ADDED COMET SUPPORT");
//            request.setAttribute("connectorRequest", request);
//        }
        next.event(request, response, event);
    }
    
}
