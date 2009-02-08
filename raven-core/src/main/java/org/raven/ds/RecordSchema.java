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
     * Creates the record based on this schema
     */
    public Record createRecord() throws RecordException;
    /**
     * Returns the record extension by its type or null if the record does not contain the
     * extension of the type passed in the parameter.
     */
    public <E> E getRecordExtension(Class<E> extensionType);
}
