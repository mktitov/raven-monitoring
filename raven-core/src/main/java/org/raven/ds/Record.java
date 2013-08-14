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
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface Record
{
    public final static String DELETE_TAG = "DELETE";

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
     * Method for groovy access to the field value
     * @see #getValue(java.lang.String)
     */
    public Object getAt(String fieldName) throws RecordException;
    /**
     * Method for groovy access to the field value
     * @see #setValue(java.lang.String, java.lang.Object) 
     */
    public void putAt(String fieldName, Object value) throws RecordException;
    /**
     * Returns the fields values as immutable map
     */
    public Map<String, Object> getValues() throws RecordException;
    /**
     * Sets values of record fields from map.
     * @param values the map with values
     * @throws RecordException 
     */
    public void setValues(Map<String, Object> values) throws RecordException;
    /**
     * Copies field value from record passed in the parameter
     */
    public void copyFrom(Record record) throws RecordException;
    /**
     * Returns the tag attached to the record or null if record does not have the tag with
     * the passed name.
     * @param tagName the name of the tag
     */
    public Object getTag(String tagName) throws RecordException;
    /**
     * Attach tag to the record.
     * @param the name of the tag
     * @param tag the tag
     */
    public void setTag(String tagName, Object tag) throws RecordException;
    /**
     * Removes the selected tag from the record.
     * @param tagName the tag name.
     */
    public void removeTag(String tagName) throws RecordException;
    /**
     * Returns <b>true</b> if record contains the tag with name passed in the parameter.
     * @param tagName the tag name
     */
    public boolean containsTag(String tagName) throws RecordException;
    /**
     * Returns the map containing the record tags. You can use this map for update operations.
     */
    public Map<String, Object> getTags() throws RecordException;
    /**
     * Returns null if the fields values passed the validation or object that holds the validation
     * errors
     */
    public RecordValidationErrors validate() throws RecordException;
    /**
     * Validates record. If record has validation errors that errors will be added as error to the 
     * <b>context</b>
     * @return <b>true</b> on success validation or <b>false</b> on validation error's
     * @throws RecordException 
     */
    public boolean validate(Node node, DataContext context) throws RecordException;
}
