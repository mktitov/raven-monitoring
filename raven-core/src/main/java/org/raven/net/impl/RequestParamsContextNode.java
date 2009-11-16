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

package org.raven.net.impl;

import java.util.Collection;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=NetworkResponseServiceNode.class, anyChildTypes=true)
public class RequestParamsContextNode extends AbstractNetworkResponseContext implements DataSource
{
    @Override
    public String doGetResponse(String requesterIp, Map<String, Object> params)
            throws NetworkResponseServiceExeption
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }
}
