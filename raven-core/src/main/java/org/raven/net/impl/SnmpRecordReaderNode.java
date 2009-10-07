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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.impl.IdRecordFieldExtension;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.table.Table;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class SnmpRecordReaderNode extends AbstractSnmpReaderNode
{
    public final static String ROW_IDS_ATTR = "row-ids";
    public final static String RECORD_SCHEMA_ATTR = "recordSchema";

    @Message
    private static String rowIdsDescription;
    @Message
    private static String recordSchemaDescription;

    @Override
    protected void proccessSnmpRequest(
            DataConsumer dataConsumer, Snmp snmp, CommunityTarget target
            , Map<String, NodeAttribute> attrs, boolean isTable)
        throws Exception
    {
        RecordSchemaNode recordSchema = attrs.get(RECORD_SCHEMA_ATTR).getRealValue();
        List<Record> records = null;
        if (!isTable)
            records = extractRecord(snmp, target, recordSchema, null);
        else
        {
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
                records = extractAllRecords(snmp, target, recordSchema);
            }
            else
            {
                records = extractRecord(snmp, target, recordSchema, ids);
            }
        }
        if (records!=null && !records.isEmpty())
        {
            for (Record record: records)
                dataConsumer.setData(this, record);
            dataConsumer.setData(this, null);
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

    private List<Record> extractAllRecords(
            Snmp snmp, CommunityTarget target, RecordSchemaNode recordSchema)
            throws Exception
    {
        SnmpRecordExtension ext = recordSchema.getRecordExtension(SnmpRecordExtension.class, null);
        if (ext==null)
            throw new Exception(String.format(
                    "Can't read snmp table because of record schema (%s) does not have (%s) " +
                    "extension", recordSchema.getPath(), SnmpRecordExtension.class.getName()));
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(ext.getOid())));
        Table table = getTableValue(snmp, target, pdu);
        String[] colNames = table.getColumnNames();
        Map<Integer, String> columns = new HashMap<Integer, String>();
        Map<String, String> oids = getFieldsOids(recordSchema, false, true);
        for (int i=0; i<colNames.length; ++i)
        {
            String fieldName = oids.get(colNames[i]);
            if (fieldName!=null)
                columns.put(i, fieldName);
        }
        Iterator<Object[]> it = table.getRowIterator();
        List<Record> records = null;
        while(it.hasNext())
        {
            Object[] row = it.next();
            Record rec = recordSchema.createRecord();
            for (int i=0; i<row.length; i++)
            {
                String fieldName = columns.get(i);
                if (fieldName!=null)
                    rec.setValue(fieldName, row[i]);
            }
            if (records==null)
                records = new ArrayList<Record>(512);
            records.add(rec);
        }
        
        return records;
    }

    private List<Record> extractRecord(
            Snmp snmp, CommunityTarget target, RecordSchemaNode recordSchema, String[] ids)
        throws Exception
    {
        RecordSchemaField[] fields = recordSchema.getFields();
        if (fields!=null && fields.length>0)
        {
            Map<String, String> oids = getFieldsOids(recordSchema, ids==null, ids!=null);
            PDU pdu = null;
            ids = ids==null? new String[]{null} : ids;
            List<Record> records = null;
            int colOidSize = 0;
            for (String id: ids)
            {
                for (String oid: oids.keySet())
                {
                    if (pdu==null)
                        pdu = new PDU();
                    OID baseOid = new OID(oid);
                    if (colOidSize==0 && id!=null)
                        colOidSize = baseOid.size();
                    if (id!=null)
                        baseOid.append(id);
                    pdu.add(new VariableBinding(baseOid));
                }
                if (pdu!=null)
                {
                    pdu.setType(PDU.GET);
                    ResponseEvent response = snmp.send(pdu, target);
                    if (response.getError()!=null)
                        throw response.getError();
                    pdu = response.getResponse();
                    if (pdu==null)
                        throw new Exception("Response timeout");
                    if (pdu.getErrorIndex()!=0)
                        throw new Exception(pdu.getErrorStatusText());
                    Vector<VariableBinding> bindings = pdu.getVariableBindings();
                    if (bindings!=null && !bindings.isEmpty())
                    {
                        Record rec = recordSchema.createRecord();
                        String indexField = oids.get(ROW_INDEX_COLUMN_NAME);
                        if (indexField!=null)
                            rec.setValue(indexField, id);
                        for (VariableBinding binding: bindings)
                        {
                            OID oid = binding.getOid();
                            if (id!=null)
                                oid = new OID(oid.getValue(), 0, colOidSize);
                            rec.setValue(oids.get(oid.toString()), binding.getVariable());
                        }

                        if (records==null)
                            records = new ArrayList<Record>();
                        records.add(rec);
                    }
                }
            }
            return records;
        }
        return null;
    }

    private Map<String, String> getFieldsOids(
            RecordSchema schema, boolean appendZero, boolean addIndexField)
    {
        RecordSchemaField[] fields = schema.getFields();
        Map<String, String> oids = null;
        if (fields!=null && fields.length>0)
        {
            PDU pdu = null;
            for (RecordSchemaField field: fields)
            {
                SnmpRecordFieldExtension snmpExt = field.getFieldExtension(
                        SnmpRecordFieldExtension.class, null);
                if (snmpExt!=null)
                {
                    if (oids==null)
                        oids = new HashMap<String, String>();
                    if (pdu==null)
                        pdu = new PDU();
                    String oidStr = snmpExt.getOid();
                    if (appendZero)
                        oidStr = oidStr.endsWith(".0")? oidStr : oidStr+".0";
                    OID oid = new OID(oidStr);
                    oids.put(oid.toString(), field.getName());
                }
                if (addIndexField)
                {
                    IdRecordFieldExtension idExt = field.getFieldExtension(
                            IdRecordFieldExtension.class, null);
                    if (idExt!=null)
                        oids.put(ROW_INDEX_COLUMN_NAME, field.getName());
                }
            }
        }
        return oids;
    }
}
