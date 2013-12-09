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

package org.raven.ds;

import java.util.Map;

/**
 *
 * @author Mikhail Titov
 */
public interface RecordSchema 
{
    /**
     * Return the record schema name.
     */
    public String getName();
    /**
     * Returns the fields of the schema.
     */
    public RecordSchemaField[] getFields();
    /**
     * Returns fields as associative array or EMPTY MAP if schema does not have fields
     */
    public Map<String, RecordSchemaField> getFieldsMap();
    /**
     * Returns the field by it's name or null if schema does not have field with specified name
     */
    public RecordSchemaField getField(String name);
    /**
     * Creates the record based on this schema
     */
    public Record createRecord() throws RecordException;
    /**
     * Creates record with initial values
     */
    public Record createRecord(Map<String, Object> values) throws RecordException;
    /**
     * Returns the record extension by its type and name or null if the record does not contain the
     * extension of the type passed in the parameter.
     * @param extensionType the type of the extension
     * @param extensionName he name of the extension. If name is null then method returns the first
     *      found extension.
     */
    public <E> E getRecordExtension(Class<E> extensionType, String extensionName);
}
