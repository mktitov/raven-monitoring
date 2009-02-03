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
public interface Record
{
    /**
     * Returns the record schema.
     */
    public RecordSchema getSchema();
    /**
     * Sets the value for selected field
     * @param fieldName the field name
     * @param value field value. The field value will be converted to the 
     *      {@link RecordSchemaField#getFieldType() type} defined in the {@link #getSchema() schema}
     * @throws org.raven.ds.RecordException if the record does not contains the field with name
     *      <code>fieldName</code> or on value converting error.
     */
    public void setValue(String fieldName, Object value) throws RecordException;
    /**
     * Returns the value of the selected field.
     * @param fieldName the field name
     * @throws org.raven.ds.RecordException if the record does not contains the field with name
     *      <code>fieldName</code>
     */
    public Object getValue(String fieldName) throws RecordException;
    /**
     * Returns the fields values as immutable map
     */
    public Map<String, Object> getValues();
}
