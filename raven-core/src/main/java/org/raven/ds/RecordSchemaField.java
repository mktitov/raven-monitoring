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

import java.util.Collection;

/**
 *
 * @author Mikhail Titov
 */
public interface RecordSchemaField extends ValueValidatorController
{
    /**
     * Returns the field name
     */
    public String getName();
    /**
     * The display name of the field
     */
    public String getDisplayName();
    /**
     * Returns the field type
     */
    public RecordSchemaFieldType getFieldType();
    /**
     * Returns the pattern which will be used when converting string to the value of this field type.
     */
    public String getPattern();
    /**
     * Returns the field extension by its type and name, or null if field does not have 
     * the extension of selected type.
     * @param extensionType the type of the extension
     * @param extensionName he name of the extension. If name is null then method returns the first
     *      found extension.
     */
    public <E> E getFieldExtension (Class<E> extensionType, String extensionName);
    /**
     * Returns the reference values source for this field or null if the field does not have
     * reference values.
     */
    public ReferenceValuesSource getReferenceValuesSource();
}
