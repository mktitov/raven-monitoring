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
package org.raven.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.RefreshAttributeValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class SQLDataSourceNode extends AbstractDataSource {

    public enum ResultType {
        SINGLE, TABLE
    };
    public final static String QUERY_ATTRIBUTE = "query";
    public final static String RESULT_TYPE_ATTRIBUTE = "queryResultType";
    
    @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private ConnectionPool connectionPool;
    @Message
    private String queryDescription;
    @Message
    private String queryResultTypeDescription;

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes) {
        NodeAttribute attr =
                new NodeAttributeImpl(QUERY_ATTRIBUTE, String.class, null, queryDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
                RESULT_TYPE_ATTRIBUTE, ResultType.class, ResultType.TABLE
                , queryResultTypeDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);
    }

    @Override
    public void gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        Connection connection = getConnection(dataConsumer);
        if (connection==null)
            return;
        try
        {
            String query = attributes.get(QUERY_ATTRIBUTE).getValue();
            ResultType resultType = attributes.get(RESULT_TYPE_ATTRIBUTE).getRealValue();
            NamedParameterStatement st = new NamedParameterStatement(connection, query);
            if (attributes!=null)
                for (NodeAttribute attr: attributes.values())
                    if (ObjectUtils.in(
                            attr.getValueHandlerType()
                            , RefreshAttributeValueHandlerFactory.TYPE
                            , QueryParameterValueHandlerFactory.TYPE))
                    {
                        st.setObject(attr.getName(), attr.getRealValue());
                    }
            ResultSet rs = st.executeQuery();
            try{
                Object result = null;
                if (resultType==ResultType.SINGLE)
                {
                    if (rs.next())
                        result = rs.getObject(1);
                }
                else
                    result = rs;
                dataConsumer.setData(this, result);
            }finally{
                if (rs!=null)
                    rs.close();
                if (st!=null)
                    st.close();
            }
        }finally{
            connection.close();
        }
        
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    private Connection getConnection(DataConsumer dataConsumer)
    {
        try {
            Connection connection = connectionPool.getConnection();
            return connection;
        } catch (Exception e) {
            logger.error(
                String.format(
                    "Error gathering data for data consumer (%s). " +
                    "Error getting connection from pool (%s)"
                    , dataConsumer.getPath(), connectionPool.getPath())
                , e);
        }
        return null;
    }
}
