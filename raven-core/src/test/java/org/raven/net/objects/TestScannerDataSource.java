/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.net.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import static org.junit.Assert.*;
/**
 *
 * @author Mikhail Titov
 */
public class TestScannerDataSource extends BaseNode implements DataSource
{
    private List<String> ips = new ArrayList<String>();

    public synchronized boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        Map<String, NodeAttribute> sessionAttributes = context.getSessionAttributes();
        assertNotNull(sessionAttributes);
        assertEquals(1, sessionAttributes.size());
        NodeAttribute attr = sessionAttributes.values().iterator().next();
        assertEquals("host", attr.getName());
        String ip = attr.getRealValue();
        assertNotNull(ip);
        ips.add(ip);
        return "10.50.1.1".equals(ip)? false : true;
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    public List<String> getIps() {
        return ips;
    }
}
