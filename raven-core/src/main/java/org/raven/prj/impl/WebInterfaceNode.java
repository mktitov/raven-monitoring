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
package org.raven.prj.impl;

import java.util.concurrent.atomic.AtomicLong;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.IfNode;
import org.raven.net.ResponseServiceNode;
import org.raven.net.impl.EventSource;
import org.raven.net.impl.FileResponseBuilder;
import org.raven.net.impl.NetworkResponseGroupNode;
import org.raven.net.impl.SimpleResponseBuilder;
import org.raven.net.impl.ZipFileResponseBuilder;
import org.raven.tree.impl.InvisibleNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = InvisibleNode.class, 
        childNodes = {
            NetworkResponseGroupNode.class, SimpleResponseBuilder.class, FileResponseBuilder.class, 
            ZipFileResponseBuilder.class, IfNode.class, EventSource.class
})
public class WebInterfaceNode extends NetworkResponseGroupNode implements ResponseServiceNode {
    public final static String NAME = "Web interface";
    
    @Parameter(readOnly=true)
    private AtomicLong requestsCount;

    @Parameter(readOnly=true)
    private AtomicLong requestsWithErrors;

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        requestsCount = new AtomicLong(0);
        requestsWithErrors = new AtomicLong(0);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        requestsCount = new AtomicLong(0);
        requestsWithErrors = new AtomicLong(0);
    }

    public WebInterfaceNode() {
        super(NAME);
    }

    public void incRequestsCountWithErrors() {
        requestsWithErrors.incrementAndGet();
    }

    public long getNextRequestId() {
        return requestsCount.incrementAndGet();
    }
}
