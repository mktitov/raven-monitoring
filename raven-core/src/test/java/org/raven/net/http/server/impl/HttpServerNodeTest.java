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
package org.raven.net.http.server.impl;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Before;
import org.junit.Test;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class HttpServerNodeTest extends RavenCoreTestCase {
    private ExecutorServiceNode executor;
    private HttpServerNode httpServer;
    
    @Before
    public void prepare() {
        executor = new ExecutorServiceNode();
        executor.setName("Executor service");
        testsNode.addAndSaveChildren(executor);        
        executor.setType(ExecutorService.Type.FORK_JOIN_POOL);
        assertTrue(executor.start());
        
        httpServer = new HttpServerNode();
        httpServer.setName("HTTP server");
        testsNode.addAndSaveChildren(httpServer);
        httpServer.setExecutor(executor);
        httpServer.setPort(7777);
        httpServer.setLogLevel(LogLevel.TRACE);
    }
    
    @Test
    public void startTest() throws InterruptedException {
        assertTrue(httpServer.start());
        Thread.sleep(30000);
        httpServer.stop();
        Thread.sleep(1000);
    }
    
    @Test
    public void invalidPathTest() throws Exception {
        assertTrue(httpServer.start());
        try (final WebClient client = new WebClient()) {
            HtmlPage p = client.getPage("http://localhost:7777");
            assertEquals(404, p.getWebResponse().getStatusCode());
            
        }
        httpServer.stop();
    }
        
}
