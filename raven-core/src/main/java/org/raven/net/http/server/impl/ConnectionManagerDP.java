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

import java.util.Iterator;
import java.util.LinkedList;
import org.raven.RavenRuntimeException;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.net.http.server.ChannelTimeoutChecker;
import org.raven.sched.ExecutorServiceException;

/**
 * Tasks:
 *   1. Manage connection keep-alive timeouts
 *   2. Manage connection read timeouts
 *   3. Manage response generation timeouts
 * @author Mikhail Titov
 */
public class ConnectionManagerDP extends AbstractDataProcessorLogic {
    public final static long TICK_TIME = 1_000L;
    public final static String CHECK_MESSAGE = "CHECK_MESSAGE";
    
    private LinkedList<ChannelTimeoutChecker> handlers = new LinkedList<>();

    @Override
    public void postInit() {
        super.postInit(); 
        try {
            getFacade().sendRepeatedly(TICK_TIME, TICK_TIME, 0, CHECK_MESSAGE);
        } catch (ExecutorServiceException ex) {
            throw new RavenRuntimeException("Error initializing Connection manager DP", ex);
        }
    }

    @Override
    public Object processData(Object message) throws Exception {
        if (message instanceof ChannelTimeoutChecker) {
            handlers.add((ChannelTimeoutChecker) message);
        } else if (message==CHECK_MESSAGE) {
            final long curTime = System.currentTimeMillis();
            ChannelTimeoutChecker checker;
            for (Iterator<ChannelTimeoutChecker> it = handlers.iterator(); it.hasNext(); ) {
                checker = it.next();
                if (checker.checkTimeoutIfNeed(curTime)) {
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("Found closed connection ({}). Removing from list", checker);
                    it.remove();
                }
            }
        }
        return VOID;
    }        
}
