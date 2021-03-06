/*
 *  Copyright 2011 Mikhail Titov.
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.raven.ds.RecordValidationErrors;

/**
 *
 * @author Mikhail Titov
 */
public class RecordValidationErrorsImpl implements RecordValidationErrors
{
    private final Map<String, Collection<String>> fieldErrors;
    private final String recordSchemaName;

    public RecordValidationErrorsImpl(String recordSchemaName)
    {
        this.recordSchemaName = recordSchemaName;
        fieldErrors = new LinkedHashMap<String, Collection<String>>();
    }

    public void addValidationErrors(String fieldName, Collection<String> errors)
    {
        fieldErrors.put(fieldName, errors);
    }

    public Map<String, Collection<String>> getErrors() {
        return fieldErrors;
    }

    public String toText() 
    {
        if (fieldErrors.isEmpty())
            return null;
        StringBuilder buf = 
                new StringBuilder("Record of schema (")
                .append(recordSchemaName)
                .append(") has validation errors: \n");
        for (Map.Entry<String, Collection<String>> errors: fieldErrors.entrySet())
        {
            buf.append(errors.getKey()).append(":\n");
            for (String error: errors.getValue())
                buf.append("  ").append(error).append("\n");
        }
        return buf.toString();
    }
}
