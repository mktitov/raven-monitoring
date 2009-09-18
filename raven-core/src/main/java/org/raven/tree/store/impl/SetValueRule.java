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

package org.raven.tree.store.impl;

import java.io.ByteArrayInputStream;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.digester.Rule;
import org.raven.tree.DataFile;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataFileValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class SetValueRule extends Rule
{
    @Override
    public void body(String namespace, String name, String text) throws Exception
    {
        super.body(namespace, name, text);
        NodeAttribute attr = (NodeAttribute) digester.peek();
        if (   DataFileValueHandlerFactory.TYPE.equals(attr.getValueHandlerType())
            && DataFile.class.isAssignableFrom(attr.getType()))
        {
            DataFile dataFile = attr.getRealValue();
            if (text==null && text.trim().isEmpty())
                dataFile.setDataStream(null);
            else
            {
                ByteArrayInputStream input = new ByteArrayInputStream(text.getBytes());
                Base64InputStream decoder = new Base64InputStream(input);
                dataFile.setDataStream(decoder);
            }
        }
        else
            attr.setValue(text);
    }
}
