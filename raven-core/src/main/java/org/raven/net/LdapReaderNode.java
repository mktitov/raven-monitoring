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

package org.raven.net;

import java.util.Collection;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class LdapReaderNode extends AbstractDataSource
{
    public final static String FILTER_ATTRIBUTE = "filter";
    public final static String ATTRIBUTES_ATTRIBUTE = "attributes";

    @Service
    protected MessagesRegistry messages;

    @NotNull @Parameter
    private String url;

    @NotNull @Parameter
    private String baseDN;

    @NotNull @Parameter
    private String userDn;

    @NotNull @Parameter
    private String userPassword;

    public String getBaseDN()
    {
        return baseDN;
    }

    public void setBaseDN(String baseDN)
    {
        this.baseDN = baseDN;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUserDn()
    {
        return userDn;
    }

    public void setUserDn(String userDn)
    {
        this.userDn = userDn;
    }

    public String getUserPassword()
    {
        return userPassword;
    }

    public void setUserPassword(String userPassword)
    {
        this.userPassword = userPassword;
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        NodeAttributeImpl attr = new NodeAttributeImpl(FILTER_ATTRIBUTE, String.class, null, null);
        attr.setDescriptionContainer(createDesc(FILTER_ATTRIBUTE));
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(ATTRIBUTES_ATTRIBUTE, String.class, null, null);
        attr.setDescriptionContainer(createDesc(ATTRIBUTES_ATTRIBUTE));
        consumerAttributes.add(attr);
    }

    private MessageComposer createDesc(String attr)
    {
        return new MessageComposer(messages).append(
                messages.createMessageKeyForStringValue(LdapReaderNode.class.getName(), attr));
    }
}
