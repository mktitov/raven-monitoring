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

import mockit.Expectations;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorFacade;
import org.raven.net.http.server.ChannelTimeoutChecker;
import org.raven.sched.ExecutorServiceException;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class ConnectionManagerDPTest {
    
    @Test
    public void initTest(
            @Mocked final DataProcessorFacade facade,
            @Mocked final DataProcessorContext ctx
    ) throws ExecutorServiceException {
        ConnectionManagerDP dp = new ConnectionManagerDP();
        dp.init(facade, ctx);
        new Verifications() {{
            facade.sendRepeatedly(ConnectionManagerDP.TICK_TIME, ConnectionManagerDP.TICK_TIME, 0, ConnectionManagerDP.CHECK_MESSAGE);
        }};
    }
    
    @Test
    public void logicTest(
            @Mocked final DataProcessorFacade facade,
            @Mocked final DataProcessorContext ctx,
            @Mocked final ChannelTimeoutChecker checker
        ) throws Exception 
    {
        new StrictExpectations() {{
            checker.checkTimeoutIfNeed(anyLong); result = false;
            checker.checkTimeoutIfNeed(anyLong); result = true;
        }};
        ConnectionManagerDP dp = new ConnectionManagerDP();
        dp.processData(checker);
        dp.processData(ConnectionManagerDP.CHECK_MESSAGE);
        dp.processData(ConnectionManagerDP.CHECK_MESSAGE);
        dp.processData(ConnectionManagerDP.CHECK_MESSAGE);
    }
    
}
