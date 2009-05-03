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
import java.util.HashMap;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.impl.SafeDataConsumer;
import org.raven.net.AccessDeniedException;
import org.raven.net.AddressMatcher;
import org.raven.net.NetworkResponseContext;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.RequiredParameterMissedException;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=NetworkResponseServiceNode.class, anyChildTypes=true)
public class NetworkResponseContextNode
        extends SafeDataConsumer implements NetworkResponseContext
{
    public static final String PARAMS_BINDING = "params";
    @NotNull() @Parameter(defaultValue="false")
    private Boolean allowRequestsFromAnyIp;

    private AddressListNode addressListNode;
    private ParametersNode parametersNode;

    public AddressListNode getAddressListNode()
    {
        return addressListNode;
    }

    public ParametersNode getParametersNode()
    {
        return parametersNode;
    }

    public Boolean getAllowRequestsFromAnyIp()
    {
        return allowRequestsFromAnyIp;
    }

    public void setAllowRequestsFromAnyIp(Boolean allowRequestsFromAnyIp)
    {
        this.allowRequestsFromAnyIp = allowRequestsFromAnyIp;
    }

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
        generateNodes();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        generateNodes();
    }

    private void checkIp(String requesterIp) throws AccessDeniedException
    {
        if (!allowRequestsFromAnyIp)
        {
            Collection<Node> addresses = addressListNode.getEffectiveChildrens();
            if (addresses!=null && !addresses.isEmpty())
                for (Node address: addresses)
                    if (address instanceof AddressMatcher)
                        if (((AddressMatcher)address).addressMatches(requesterIp))
                            return;
            throw new AccessDeniedException();
        }
    }

    private Map<String, Object> checkParameters(Map<String, Object> params)
            throws RequiredParameterMissedException
    {
        Map<String, Object> newParams = new HashMap<String, Object>();
        Collection<Node> paramsList = parametersNode.getEffectiveChildrens();
        if (paramsList!=null && !paramsList.isEmpty())
            for (Node node: paramsList)
                if (node.getStatus().equals(Status.STARTED) && node instanceof ParameterNode)
                {
                    ParameterNode param = (ParameterNode) node;
                    Object value = params==null? null : params.get(param.getName());
                    if (value==null && param.getRequired())
                        throw new RequiredParameterMissedException(param.getName(), getName());
                    value = converter.convert(param.getParameterType(), value, param.getPattern());
                    if (value!=null)
                        newParams.put(param.getName(), value);
                }

        return newParams;
    }

    private void generateNodes()
    {
        addressListNode = (AddressListNode) getChildren(AddressListNode.NAME);
        if (addressListNode==null)
        {
            addressListNode = new AddressListNode();
            this.addAndSaveChildren(addressListNode);
            addressListNode.start();
        }

        parametersNode = (ParametersNode) getChildren(ParametersNode.NAME);
        if (parametersNode==null)
        {
            parametersNode = new ParametersNode();
            this.addAndSaveChildren(parametersNode);
            parametersNode.start();
        }
    }

    public String getResponse(String requesterIp, Map<String, Object> params)
            throws NetworkResponseServiceExeption
    {
        checkIp(requesterIp);
        params = checkParameters(params);
        bindingSupport.put(PARAMS_BINDING, params);
        Object value = refereshData(null);
        String result = converter.convert(String.class, value, null);
        
        return result;
    }
}
