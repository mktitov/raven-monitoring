/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.sched.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.AbstractAttributeValueHandler;

/**
 *
 * @author Mikhail Titov
 */
public class TimeZoneValueHandler extends AbstractAttributeValueHandler
{
    private TimeZone timeZone;
    private final static Set<String> TIMEZONES_SET = 
                new HashSet<String>(Arrays.asList(TimeZone.getAvailableIDs()));

    public TimeZoneValueHandler(NodeAttribute attribute) {
        super(attribute);
    }
    
    public void setData(String value) throws Exception {
        
    }

    public String getData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object handleData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void close() {
    }

    public boolean isReferenceValuesSupported() {
        return true;
    }

    public boolean isExpressionSupported() {
        return false;
    }

    public boolean isExpressionValid() {
        return true;
    }

    public void validateExpression() throws Exception {
    }
    
}
