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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.apache.commons.lang.StringUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCompiler;
import org.raven.expr.impl.ExpressionAttributeValueHandler;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.table.ColumnBasedTable;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.expr.impl.BindingSupportImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class LdapReaderNode extends AbstractDataSource
{
    public final static String FILTER_ATTRIBUTE = "filter";
    public final static String ATTRIBUTES_ATTRIBUTE = "attributes";
    public final static String FETCH_ATTRIBUTES_ATTRIBUTE = "fetchAttributes";
    public final static String START_SEARCH_FROM_ATTRIBUTE = "startFetchFromDN";
    public final static String ADD_OBJECTDN_TO_RESULT_ATTRIBUTE = "addObjectDNToResult";
    public final static String ROW_FILTER_ATTRIBUTE = "rowFilter";
    public final static String USE_ROW_FILTER_ATTRIBUTE = "useRowFilter";

    public static final String OBJECTDN_COLUMN_NAME = "objectDN";
    
    @Service
    protected static MessagesRegistry messages;

    @Service
    protected static ExpressionCompiler expressionCompiler;

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

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

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
            DataConsumer dataConsumer, DataContext dataContext) throws Exception
    {
        Map<String, NodeAttribute> attributes = dataContext.getSessionAttributes();
        String filter = attributes.get(FILTER_ATTRIBUTE).getValue();
        if (filter==null)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(String.format(
                        "Error making request for (%s). Filter attribute value can not be null"
                        , dataConsumer.getPath()));
            return false;
        }
        
        NodeAttribute fetchAttrsAttr = attributes.get(FETCH_ATTRIBUTES_ATTRIBUTE);
        Boolean fetchAttrs = fetchAttrsAttr == null ?
            Boolean.FALSE: (Boolean) fetchAttrsAttr.getRealValue();
        String[] attrs = new String[]{};
        if (fetchAttrs)
        {
            String attrsList = getAttributeValue(ATTRIBUTES_ATTRIBUTE, attributes);
            if (attrsList!=null && !attrsList.trim().isEmpty())
                attrs = attrsList.split("\\s+");
            else
                attrs = null;
        }
        String startFromDN = getAttributeValue(START_SEARCH_FROM_ATTRIBUTE, attributes);
        if (startFromDN==null || startFromDN.trim().isEmpty())
            startFromDN = baseDN;

        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Connecting to LDAP server");
        DirContext context = createContext(startFromDN);
        Boolean addObjectDNToResult =
                attributes.get(ADD_OBJECTDN_TO_RESULT_ATTRIBUTE).getRealValue();
        if (isLogLevelEnabled(LogLevel.DEBUG))
        {
            debug("Connected");
            String attrsStr = "";
            if (attrs==null)
                attrsStr = "ALL";
            else if (attrs.length==0)
                attrsStr = "DON'T FETCH";
            else
                attrsStr = StringUtils.join(attrs, ", ");
            debug(String.format(
                    "LDAP request parameters: \n  addObjectDNToResult (%s)\n  fetchAttributes (%s)" +
                    "\n  startFromDN (%s)\n  filter >>>%s<<<\n  attributes (%s)"
                    , addObjectDNToResult, fetchAttrs, startFromDN, filter, attrsStr));
        }
        ColumnBasedTable table = null;
        boolean useRowFilter = (Boolean)attributes.get(USE_ROW_FILTER_ATTRIBUTE).getRealValue();
        NodeAttribute rowFilterAttr = attributes.get(ROW_FILTER_ATTRIBUTE);
        Expression filterExpression = null;
        if (   useRowFilter && rowFilterAttr!=null && rowFilterAttr.getRawValue()!=null
            && ExpressionAttributeValueHandlerFactory.TYPE.equals(
                    rowFilterAttr.getValueHandlerType()))
        {
            filterExpression =
                    ((ExpressionAttributeValueHandler)rowFilterAttr.getValueHandler())
                    .getExpression();
        }
        try
        {
            SearchControls control = new SearchControls();
            control.setSearchScope(SearchControls.SUBTREE_SCOPE);
            control.setReturningAttributes(attrs);
            control.setReturningObjFlag(true);
            NamingEnumeration<SearchResult> answer = context.search("", filter, control);
            table = new ColumnBasedTable();
            int rowsCount = 0;
            Map<String, Object> row = null;
            while (answer.hasMore())
            {
                row = new HashMap<String, Object>();
                List<String> colNames = new ArrayList<String>(16);
                SearchResult searchResult = answer.next();
                if (isLogLevelEnabled(LogLevel.TRACE))
                    trace(String.format("row [%d] result: %s", rowsCount, searchResult.toString()));
                if (addObjectDNToResult)
                {
                    row.put(OBJECTDN_COLUMN_NAME, searchResult.getNameInNamespace());
                    colNames.add(OBJECTDN_COLUMN_NAME);
                }
                Attributes objAttrs = searchResult.getAttributes();
                NamingEnumeration<? extends Attribute> attrsIterator = objAttrs.getAll();
                while (attrsIterator.hasMore()) {
                    Attribute objAttr = attrsIterator.next();
                    NamingEnumeration values = objAttr.getAll();
//                    objAttr.getAttributeDefinition().
//                    boolean firstCycle=true;
//                    StringBuilder value = new StringBuilder();
                    LinkedList<String> valuesList = new LinkedList<String>();
                    while (values.hasMore())
                        valuesList.add(values.next().toString());
//                    {
//                        if (!firstCycle)
//                            value.append(", ");
//                        value.append(values.next());
//                        if (firstCycle)
//                            firstCycle=false;
//                    }
                    Object value = valuesList.isEmpty()? null : 
                            (valuesList.size()==1? valuesList.getFirst() : valuesList);
                    row.put(objAttr.getID(),value);
                    colNames.add(objAttr.getID());
                }
                boolean skipRow = false;
                if (useRowFilter)
                {
                    if (rowFilterAttr==null)
                        skipRow = true;
                    else if (filterExpression!=null)
                    {
                        SimpleBindings bindings = new SimpleBindings();
                        rowFilterAttr.getOwner().formExpressionBindings(bindings);
                        bindings.put("row", row);
                        skipRow = !converter.convert(
                                Boolean.class, filterExpression.eval(bindings), null);
                    }
                    else
                        skipRow = !Boolean.TRUE.equals(rowFilterAttr.getRealValue());
                }
                if (!skipRow && !row.isEmpty())
                {
                    ++rowsCount;
                    for (String colName: colNames)
                        table.addValue(colName, row.get(colName));
                }

            }
            if (rowsCount>0)
                table.freeze();
            else
                table = null;
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Fetched (%d) rows", rowsCount));
            dataConsumer.setData(this, table, dataContext);
            
        }finally{
            context.close();
        }
        
        return true;
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
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
//        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(ATTRIBUTES_ATTRIBUTE, String.class, null, null);
        attr.setDescriptionContainer(createDesc(ATTRIBUTES_ATTRIBUTE));
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(FETCH_ATTRIBUTES_ATTRIBUTE, Boolean.class, false, null);
        attr.setDescriptionContainer(createDesc(FETCH_ATTRIBUTES_ATTRIBUTE));
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(START_SEARCH_FROM_ATTRIBUTE, String.class, null, null);
        attr.setDescriptionContainer(createDesc(START_SEARCH_FROM_ATTRIBUTE));
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(ADD_OBJECTDN_TO_RESULT_ATTRIBUTE, Boolean.class, false, null);
        attr.setDescriptionContainer(createDesc(ADD_OBJECTDN_TO_RESULT_ATTRIBUTE));
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(ROW_FILTER_ATTRIBUTE, Boolean.class, null, null);
        attr.setDescriptionContainer(createDesc(ROW_FILTER_ATTRIBUTE));
        attr.setRequired(false);
        consumerAttributes.add(attr);
        
        attr = new NodeAttributeImpl(USE_ROW_FILTER_ATTRIBUTE, Boolean.class, false, null);
        attr.setDescriptionContainer(createDesc(USE_ROW_FILTER_ATTRIBUTE));
        attr.setRequired(true);
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
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, authType);
        env.put(Context.SECURITY_PRINCIPAL, userDN);
        env.put(Context.SECURITY_CREDENTIALS, userPassword);

        InitialDirContext initialContext = new InitialDirContext(env);
        DirContext context = (DirContext) initialContext.lookup(startDN);
        
        return context;
    }
}
