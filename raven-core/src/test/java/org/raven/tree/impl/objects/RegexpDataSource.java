/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.tree.impl.objects;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class RegexpDataSource extends AbstractDataSource
{

    private String stringData;
    private String charset;

    public String getStringData()
    {
        return stringData;
    }

    public void setStringData(String stringData)
    {
        this.stringData = stringData;
    }

    public String getCharset()
    {
        return charset;
    }

    public void setCharset(String charset)
    {
        this.charset = charset;
    }

    @Override
    public boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context)
            throws Exception
    {
        ByteArrayInputStream is = new ByteArrayInputStream(stringData.getBytes());

        dataConsumer.setData(this, is, context);
        
        return true;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }
}
