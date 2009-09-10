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

import org.raven.annotations.NodeClass;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemasNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class, childNodes={RecordSchemaFieldNode.class})
public class SyslogRecordSchemaNode extends RecordSchemaNode
{
    public static final String DATE_FIELD = "date";
    public static final String FACILITY_FIELD = "facility";
    public static final String HOST_FIELD = "host";
    public static final String LEVEL_FIELD = "level";
    public static final String MESSAGE_FIELD = "message";
    
    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
        initializeSchema();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        initializeSchema();
    }

    private void initializeSchema()
    {
        createField(DATE_FIELD, RecordSchemaFieldType.TIMESTAMP, "dd.MM.yyyy HH:mm:ss");
        createField(HOST_FIELD, RecordSchemaFieldType.STRING, null);
        createField(FACILITY_FIELD, RecordSchemaFieldType.STRING, null);
        createField(LEVEL_FIELD, RecordSchemaFieldType.STRING, null);
        createField(MESSAGE_FIELD, RecordSchemaFieldType.STRING, null);
    }

//    private void createField(String name, RecordSchemaFieldType fieldType, String format)
//    {
//        if (getChildren(name)!=null)
//            return;
//        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
//        field.setName(name);
//        addAndSaveChildren(field);
//        field.setDisplayName(StringUtils.capitalize(name));
//        field.setFieldType(fieldType);
//        field.setPattern(format);
//        field.start();
//    }
}
