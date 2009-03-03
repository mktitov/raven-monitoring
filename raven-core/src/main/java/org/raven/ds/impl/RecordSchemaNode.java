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

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class)
public class RecordSchemaNode extends BaseNode implements RecordSchema
{
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode extendsSchema;

    @Parameter
    private String includeFields;
    @Parameter
    private String excludeFields;

    private RecordExtensionsNode recordExtensionsNode;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();

        recordExtensionsNode = (RecordExtensionsNode) getChildren(RecordExtensionsNode.NAME);
        if (recordExtensionsNode==null)
        {
            recordExtensionsNode = new RecordExtensionsNode();
            this.addAndSaveChildren(recordExtensionsNode);
            recordExtensionsNode.start();
        }
    }

    public RecordSchemaField[] getFields()
    {
        List<RecordSchemaField> fields = new ArrayList<RecordSchemaField>(32);
        if (extendsSchema!=null)
        {
            RecordSchemaField[] parentFields = extendsSchema.getFields();
            if (parentFields!=null)
            {
                if (includeFields!=null)
                {
                    String[] includeFieldsArr = splitAndSort(includeFields);
                    if (includeFieldsArr!=null && includeFieldsArr.length>0)
                    {
                        for (RecordSchemaField field: parentFields)
                            if (Arrays.binarySearch(includeFieldsArr, field.getName())>=0)
                                fields.add(field);
                    }
                }
                else if (excludeFields!=null)
                {
                    String[] excludeFieldsArr =splitAndSort(excludeFields);
                    if (excludeFieldsArr!=null && excludeFieldsArr.length>0)
                    {
                        for (RecordSchemaField field: parentFields)
                            if (Arrays.binarySearch(excludeFieldsArr, field.getName())<0)
                                fields.add(field);
                    }
                }
                else
                    for (RecordSchemaField field: parentFields)
                        fields.add(field);
            }
        }
        Collection<Node> childs = getSortedChildrens();
        if (childs!=null || childs.size()>0)
            for (Node child: childs)
                if (child instanceof RecordSchemaField && child.getStatus()==Status.STARTED)
                    fields.add((RecordSchemaField)child);

        if (fields.size()==0)
            return null;

        Set<String> fieldNames = new HashSet<String>();
        ListIterator<RecordSchemaField> it = fields.listIterator(fields.size());
        for (; it.hasPrevious();)
        {
            RecordSchemaField field = it.previous();
            if (fieldNames.contains(field.getName()))
                it.remove();
            else
                fieldNames.add(field.getName());
        }

        RecordSchemaField[] result = new RecordSchemaField[fields.size()];
        fields.toArray(result);

        return result;
    }

    public Record createRecord() throws RecordException
    {
        return new RecordImpl(this);
    }

    public <E> E getRecordExtension(Class<E> extensionType, String extensionName)
    {
        Collection<Node> childs = recordExtensionsNode.getChildrens();
        if (childs!=null && childs.size()>0)
            for (Node child: childs)
                if (   Status.STARTED==child.getStatus()
                    && extensionType.isAssignableFrom(child.getClass())
                    && (extensionName==null || extensionName.equals(child.getName())))
                {
                    return (E)child;
                }

        RecordSchemaNode _extendsSchema = extendsSchema;
        if (_extendsSchema!=null)
            return _extendsSchema.getRecordExtension(extensionType, extensionName);

        return null;
    }

    public RecordExtensionsNode getRecordExtensionsNode()
    {
        return recordExtensionsNode;
    }

    public String getExcludeFields()
    {
        return excludeFields;
    }

    public void setExcludeFields(String excludeFields)
    {
        this.excludeFields = excludeFields;
    }

    public RecordSchemaNode getExtendsSchema()
    {
        return extendsSchema;
    }

    public void setExtendsSchema(RecordSchemaNode extendsSchema)
    {
        this.extendsSchema = extendsSchema;
    }

    public String getIncludeFields()
    {
        return includeFields;
    }

    public void setIncludeFields(String includeFields)
    {
        this.includeFields = includeFields;
    }

    private String[] splitAndSort(String str)
    {
        StrTokenizer tokenizer = new StrTokenizer(str, ',');
        tokenizer.setTrimmerMatcher(StrMatcher.trimMatcher());
        String[] result = tokenizer.getTokenArray();
        Arrays.sort(result);
        return result;
    }
}
