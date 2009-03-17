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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.raven.ds.BinaryFieldType;
import org.raven.ds.BinaryFieldTypeException;

/**
 *
 * @author Mikhail Titov
 */
public class BinaryFieldValue implements BinaryFieldType
{
    private final byte[] data;

    public BinaryFieldValue(byte[] data)
    {
        this.data = data;
    }

    public InputStream getData() throws BinaryFieldTypeException
    {
        return new ByteArrayInputStream(data);
    }

    public void closeResources() throws BinaryFieldTypeException
    {
    }

}
