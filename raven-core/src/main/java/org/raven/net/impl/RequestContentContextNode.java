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

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=NetworkResponseServiceNode.class, anyChildTypes=true)
public class RequestContentContextNode
        extends AbstractNetworkResponseContext implements DataSource
{
    @Override
    protected void generateNodes()
    {
        super.generateNodes();

        ParameterNode contentParam = (ParameterNode) parametersNode.getChildren(
                NetworkResponseService.REQUEST_CONTENT_PARAMETER);
        if (contentParam==null)
            addParameter(
                NetworkResponseService.REQUEST_CONTENT_PARAMETER, InputStream.class, true, null);
    }
    
    @Override
    public String doGetResponse(String requesterIp, Map<String, Object> params)
            throws NetworkResponseServiceExeption
    {
        Object requestContent = params.get(NetworkResponseService.REQUEST_CONTENT_PARAMETER);
        Collection<Node> deps = getDependentNodes();
        if (deps!=null && !deps.isEmpty())
            for (Node dep: deps)
                if (dep instanceof DataConsumer && dep.getStatus().equals(Status.STARTED))
                    ((DataConsumer)dep).setData(this, requestContent);
        return null;
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }
}
