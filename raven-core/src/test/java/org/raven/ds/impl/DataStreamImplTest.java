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
package org.raven.ds.impl;

import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class DataStreamImplTest extends Assert {
    
    @Test
    public void testWithDataConsumer(
            @Mocked final DataConsumer consumer,
            @Mocked final DataSource source,
            @Mocked final DataContext context
        ) 
    {
        DataStreamImpl stream = new DataStreamImpl(source, context, consumer);
        assertSame(stream, stream.push("test"));
        new Verifications() {{
            consumer.setData(source, "test", context);
        }};
    }
    
    @Test
    public void testWithoutDataConsumer(
            @Mocked final DataConsumer consumer,
            @Mocked final DataSource source,
            @Mocked final DataContext context,
            @Mocked final DataSourceHelper helper
        )
    {
        DataStreamImpl stream = new DataStreamImpl(source, context);
        assertSame(stream, stream.push("test"));
        new Verifications() {{
            DataSourceHelper.sendDataToConsumers(source, "test", context);
        }};
    }
}
