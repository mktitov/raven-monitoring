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

package org.raven.net.impl;

import java.util.Collection;
import java.util.Map;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.snmp4j.Snmp;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public class SnmpRecordReaderNode extends AbstractSnmpReaderNode
{
    public final static String ROW_IDS_ATTR = "row-ids";
    public final static String RECORD_SCHEMA_ATTR = "recordSchema";

    @Message
    private static String rowIdsDescription;
    @Message
    private static String recordSchemaDescription;

    @Override
    protected void proccessSnmpRequest(Snmp snmp, Map<String, NodeAttribute> attrs, boolean isTable)
            throws Exception
    {
        RecordSchemaNode recordSchema = attrs.get(RECORD_SCHEMA_ATTR).getRealValue();
        String[] ids=null;
        NodeAttribute idsAttr = attrs.get(ROW_IDS_ATTR);
        if (idsAttr!=null)
        {
            String val = idsAttr.getValue();
            if (val!=null)
            {
                ids = val.split("\\s*,\\s*");
            }
        }
        if (ids==null || ids.length==0)
        {
            extractAllRecords(snmp, recordSchema);
        }
        else
        {

        }
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        try
        {
            super.fillConsumerAttributes(consumerAttributes);
            
            consumerAttributes.add(new NodeAttributeImpl(
                    ROW_IDS_ATTR, String.class, null, rowIdsDescription));
            
            NodeAttributeImpl attr = new NodeAttributeImpl(
                    RECORD_SCHEMA_ATTR, RecordSchemaNode.class, null, recordSchemaDescription);
            attr.setValueHandlerType(RecordSchemaValueTypeHandlerFactory.TYPE);
            attr.setRequired(true);
            consumerAttributes.add(attr);
        }
        catch (Exception ex)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error generating attributes for consumer", ex);
        }
    }

    private void extractAllRecords(Snmp snmp, RecordSchemaNode recordSchema)
    {
        
    }
}
