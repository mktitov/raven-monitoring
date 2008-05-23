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

package org.raven.snmp;

import java.util.Collection;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class SnmpNode extends AbstractDataSource
{

    public void getDataImmediate(DataConsumer dataConsumer)
    {
        ;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        NodeAttributeImpl attr = 
                new NodeAttributeImpl(
                    "host", String.class, null, "The ip address or the domain name of the device");
        attr.setRequired(true);
        consumerAttributes.add(attr);
        
        attr = new NodeAttributeImpl("OID", String.class, null, "The Object Identifier Class");
        attr.setRequired(true);
        consumerAttributes.add(attr);
        
//        attr = new NodeAttributeImpl("SNMP port", Integer.class, 161)
    }

}
