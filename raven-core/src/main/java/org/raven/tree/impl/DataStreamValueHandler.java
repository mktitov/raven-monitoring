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

package org.raven.tree.impl;

import java.io.InputStream;
import org.raven.tree.DataStream;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class DataStreamValueHandler extends AbstractAttributeValueHandler implements DataStream
{
    public DataStreamValueHandler(NodeAttribute attribute) throws DataStreamValueHandlerException
    {
        super(attribute);
        if (!DataStream.class.isAssignableFrom(attribute.getType()))
            throw new DataStreamValueHandlerException(String.format(
                    "Invalid attribute (%s) type (%s). Type must be assignable from (%s)"
                    , attribute.getPath()
                    , attribute.getType().getName(), DataStream.class.getName()));
    }

    public void setData(String value) throws Exception {
    }

    public String getData() {
        return null;
    }

    public Object handleData() {
        return this;
    }

    public void close() {
    }

    public boolean isReferenceValuesSupported() {
        return false;
    }

    public boolean isExpressionSupported() {
        return true;
    }

    public boolean isExpressionValid() {
        return true;
    }

    public void validateExpression() throws Exception {
    }

    public void setStream(InputStream stream) {
        fireValueChangedEvent(null, stream);
    }

    @Override
    public String toString() {
        return "";
    }
}
