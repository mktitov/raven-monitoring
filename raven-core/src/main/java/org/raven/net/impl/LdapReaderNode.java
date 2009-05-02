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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
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
    public final static String FETCH_ATTRIBUTES_ATTRIBUTE = "fetchAttributes";
    public final static String START_SEARCH_FROM = "startFetchFromDN";

    @Service
    protected MessagesRegistry messages;

    @NotNull @Parameter
    private String url;

    @NotNull @Parameter
    private String baseDN;

    @NotNull @Parameter
    private String userDN;

    @NotNull @Parameter
    private String userPassword;

    @NotNull @Parameter(defaultValue="simple")
    private String authType;

    public String getAuthType()
    {
        return authType;
    }

    public void setAuthType(String authType)
    {
        this.authType = authType;
    }

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

    public String getUserDN()
    {
        return userDN;
    }

    public void setUserDN(String userDn)
    {
        this.userDN = userDn;
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
        NodeAttribute fetchAttrsAttr = attributes.get(FETCH_ATTRIBUTES_ATTRIBUTE);
        Boolean fetchAttrs = fetchAttrsAttr == null ?
            Boolean.FALSE: (Boolean) fetchAttrsAttr.getRealValue();
        String[] attrs = null;
        if (fetchAttrs)
        {
            String attrsList = getAttributeValue(ATTRIBUTES_ATTRIBUTE, attributes);
            if (attrsList!=null && !attrsList.trim().isEmpty())
                attrs = attrsList.split("\\s+");
            else
                attrs = new String[]{};
        }
        String startFromDN = getAttributeValue(START_SEARCH_FROM, attributes);
        if (startFromDN==null || startFromDN.trim().isEmpty())
            startFromDN = baseDN;

        DirContext context = createContext(startFromDN);
        try{
            SearchControls control = new SearchControls();
            control.setSearchScope(SearchControls.SUBTREE_SCOPE);
            control.setReturningAttributes(attrs);
            control.setReturningObjFlag(true);
//            NamingEnumeration answer = context.search("", query, control);
//            lines = new LinkedList();
//            while (answer.hasMore()){
//                Binding elem = (Binding)answer.next();
//                if (elem.getObject() instanceof DirContext)
//                    lines.add(formLine((DirContext)elem.getObject(), false));
//            }
        }finally{
            context.close();
        }
        
        return true;
    }

    private String getAttributeValue(String attrName, Map<String, NodeAttribute> attrs)
    {
        NodeAttribute attr = attrs.get(attrName);
        return attr==null? null : attr.getValue();
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

        attr = new NodeAttributeImpl(FETCH_ATTRIBUTES_ATTRIBUTE, Boolean.class, false, null);
        attr.setDescriptionContainer(createDesc(FETCH_ATTRIBUTES_ATTRIBUTE));
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(START_SEARCH_FROM, String.class, null, null);
        attr.setDescriptionContainer(createDesc(START_SEARCH_FROM));
        consumerAttributes.add(attr);
    }

    private MessageComposer createDesc(String attr)
    {
        return new MessageComposer(messages).append(
                messages.createMessageKeyForStringValue(LdapReaderNode.class.getName(), attr));
    }

    public DirContext createContext(String startDN) throws NamingException
    {
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, authType);
        env.put(Context.SECURITY_PRINCIPAL, "komi_resin");
        env.put(Context.SECURITY_CREDENTIALS, userPassword);

        InitialDirContext initialContext = new InitialDirContext(env);
        DirContext context = (DirContext) initialContext.lookup(startDN);
        
        return context;
    }
}
