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

package org.raven.tree.store.impl;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.store.TreeStore;
import org.raven.tree.store.TreeStoreError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
//TODO: add required property to NodeAttribute functionality
//TODO: add autoStart to the Node functionality
public class H2TreeStore implements TreeStore
{
	private final static Logger logger = LoggerFactory.getLogger(TreeStore.class);

    @Service
    private static MessagesRegistry messagesRegistry;

    public static final int GET_NODES_FETCH_SIZE = 1000;
    public final static String NODES_TABLE_NAME = "NODES" ;
    public final static String NODE_ATTRIBUTES_TABLE_NAME = "NODE_ATTRIBUTES";
    public final static String NODE_ATTRIBUTES_BINARY_DATA_TABLE_NAME
            = "NODE_ATTRIBUTES_BINARY_DATA";

    private Connection connection; 
    
    private PreparedStatement insertNodeStatement;
    private PreparedStatement selectNodeStatement;
    private PreparedStatement updateNodeStatement;
    private PreparedStatement removeNodeStatement;
    private PreparedStatement insertNodeAttributeStatement;
    private PreparedStatement updateNodeAttributeStatement;
    private PreparedStatement removeNodeAttributeStatement;
    private PreparedStatement selectNodeAttributesStatement;
    private PreparedStatement hasBinaryDataStatement;
    private PreparedStatement insertBinaryDataStatement;
    private PreparedStatement getBinaryDataStatement;
    private PreparedStatement updateBinaryDataStatement;
    private PreparedStatement removeBinaryDataStatement;

    private int dynamicNodeId=0;
    
    public void init(String databaseUrl, String username, String password) throws TreeStoreError
    {
        try
        {
            initConnection(databaseUrl, username, password);
            initTables();
        }
        catch (Exception ex)
        {
            throw new TreeStoreError("H2 Tree store initialization error", ex);
        }
    }

    public synchronized void saveNode(Node node) throws TreeStoreError
    {
        try
        {
            try
            {
                if (node.getId()==0)
                    insertNode(node);
                else
                    updateNode(node);

                connection.commit();

            }catch(Exception e)
            {
                connection.rollback();
            }
        } catch (SQLException ex)
        {
            throw new TreeStoreError(
                    String.format(
                        "Error saving node %s", node.getPath())
                    , ex);
        }
    }

    public synchronized Node getNode(int id) throws TreeStoreError
    {
        try
        {
            if (selectNodeStatement == null)
            {
                selectNodeStatement = connection.prepareStatement(
                        String.format(
                            "select id, owner, index, name, node_type " +
                            "from %s " +
                            "where id=?"
                        , NODES_TABLE_NAME));
            }
            
            selectNodeStatement.setInt(1, id);
            
            ResultSet rs = selectNodeStatement.executeQuery();
            try
            {
                if (rs.next())
                    return createNode(rs, null);
                else
                    return null;
            }finally
            {
                rs.close();
                connection.commit();
            }
            
        } catch (Exception e)
        {
            throw new TreeStoreError(
                    String.format("Error getting node by id (%d)", id)
                    , e);
        }
    }

    public synchronized void removeNode(int id) throws TreeStoreError
    {
        try
        {
            try
            {
                if (removeNodeStatement == null)
                {
                    removeNodeStatement = connection.prepareStatement(
                            String.format(
                            "delete from %s where id=?", NODES_TABLE_NAME));
                }
                removeNodeStatement.setInt(1, id);
                removeNodeStatement.executeUpdate();

                connection.commit();
                
            }catch(Exception e)
            {
                connection.rollback();
                throw e;
            }
        } catch (Exception e)
        {
            throw new TreeStoreError(
                    String.format("Error deleting node by id (%d)", id)
                    , e);
        }
    }

    public synchronized void saveNodeAttribute(NodeAttribute nodeAttribute) throws TreeStoreError
    {
        if (nodeAttribute.isReadonly())
            return;
        try
        {
            try
            {
                if (nodeAttribute.getId()==0)
                    insertNodeAttribute(nodeAttribute);
                else
                    updateNodeAttribute(nodeAttribute);
                
                connection.commit();
                
            }catch(Exception e)
            {
                connection.rollback();
                throw e;
            }
        } catch (Exception ex)
        {
            throw new TreeStoreError(
                    String.format(
                        "Error saving node (%s) attribute (%s)"
                        , nodeAttribute.getOwner().getPath(), nodeAttribute.getName())
                    , ex);
        }
    }

    public synchronized InputStream getNodeAttributeBinaryData(NodeAttribute attr)
    {
        try
        {
            try
            {
                if (getBinaryDataStatement==null)
                    getBinaryDataStatement = connection.prepareStatement(String .format(
                            "select data from %s where id=?"
                            , NODE_ATTRIBUTES_BINARY_DATA_TABLE_NAME));
                getBinaryDataStatement.setInt(1, attr.getId());
                ResultSet rs = getBinaryDataStatement.executeQuery();
                try
                {
                    if (rs.next())
                        return rs.getBinaryStream(1);
                    else
                        return null;
                }
                finally
                {
                    rs.close();
                    connection.commit();
                }
            }
            catch(Exception e)
            {
                connection.rollback();
                throw e;
            }
        }
        catch(Exception e)
        {
            throw new TreeStoreError(
                    String.format(
                        "Error getting binary data for attribute (%s)"
                        , attr.getPath())
                    , e);

        }
    }

    public synchronized void saveNodeAttributeBinaryData(NodeAttribute attr, InputStream data)
    {
        try
        {
            try
            {
                if (data==null)
                    removeNodeAttributeBinaryData(attr);
                if (!hasNodeAttributeBinaryData(attr))
                    insertNodeAttributeBinaryData(attr, data);
                else
                    updateNodeAttributeBinaryData(attr, data);
            }
            catch (Exception e)
            {
                connection.rollback();
                throw e;
            }
        }
        catch(Exception e)
        {
            throw new TreeStoreError(
                    String.format(
                        "Error saveing binary data of the attribute (%s)"
                        , attr.getPath())
                    , e);
            
        }
    }

    private void insertNodeAttributeBinaryData(NodeAttribute attr, InputStream data)
            throws SQLException
    {
        if (insertBinaryDataStatement==null)
            insertBinaryDataStatement = connection.prepareStatement(String.format(
                    "insert into %s (id, data) values (?,?)"
                    , NODE_ATTRIBUTES_BINARY_DATA_TABLE_NAME));
        insertBinaryDataStatement.setInt(1, attr.getId());
        insertBinaryDataStatement.setBinaryStream(2, data);
        insertBinaryDataStatement.executeUpdate();
    }

    private void updateNodeAttributeBinaryData(NodeAttribute attr, InputStream data) throws SQLException
    {
        if (updateBinaryDataStatement==null)
            updateBinaryDataStatement = connection.prepareStatement(String.format(
                    "update %s set data=? where id=?", NODE_ATTRIBUTES_BINARY_DATA_TABLE_NAME));
        updateBinaryDataStatement.setBinaryStream(1, data);
        updateBinaryDataStatement.setInt(2, attr.getId());
        updateBinaryDataStatement.executeUpdate();
    }
    
    public synchronized boolean hasNodeAttributeBinaryData(NodeAttribute attr)
    {
        try
        {
            try
            {
                if (hasBinaryDataStatement==null)
                    hasBinaryDataStatement = connection.prepareStatement(String.format(
                            "select count(*) from %s where id=?"
                            , NODE_ATTRIBUTES_BINARY_DATA_TABLE_NAME));
                hasBinaryDataStatement.setInt(1, attr.getId());
                ResultSet rs = hasBinaryDataStatement.executeQuery();
                try
                {
                    rs.next();
                    return rs.getInt(1)>0;
                }
                finally
                {
                    rs.close();
                    connection.commit();
                }
            }
            catch(Exception e)
            {
                connection.rollback();
                throw e;
            }
        }
        catch (Exception ex)
        {
            throw new TreeStoreError(
                    String.format(
                        "Error getting information about binary data for attribute (%s)"
                        , attr.getPath())
                    , ex);
        }
    }

    public synchronized void removeNodeAttribute(int id) throws TreeStoreError
    {
        try
        {
            try
            {
                if (removeNodeAttributeStatement == null)
                {
                    removeNodeAttributeStatement = connection.prepareStatement(
                            String.format(
                            "delete from %s where id=?", NODE_ATTRIBUTES_TABLE_NAME));
                }
                removeNodeAttributeStatement.setInt(1, id);
                removeNodeAttributeStatement.executeUpdate();

                connection.commit();
            }catch(Exception e)
            {
                connection.rollback();
                throw e;
            }
        } catch (Exception e)
        {
            throw new TreeStoreError(
                    String.format(
                        "Error removing attribute with id (%d)", id)
                    , e);
        }
    }

    public synchronized void removeNodeAttributeBinaryData(NodeAttribute attr)
    {
        try
        {
            try
            {
                if (removeBinaryDataStatement==null)
                    removeBinaryDataStatement = connection.prepareStatement(String.format(
                            "delete from %s where id=?", NODE_ATTRIBUTES_BINARY_DATA_TABLE_NAME));
                removeBinaryDataStatement.setInt(1, attr.getId());
                removeBinaryDataStatement.executeUpdate();
                connection.commit();
            }
            catch(Exception e)
            {
                connection.rollback();
                throw e;
            }
        }
        catch (Exception e)
        {
            throw new TreeStoreError(
                    String.format(
                        "Error removing binary data of the attribute (%s)", attr.getPath())
                    , e);
            
        }
    }

    public synchronized Node getRootNode() throws TreeStoreError
    {
        try
        {
            if (logger.isInfoEnabled())
                logger.info("Loading all nodes from store");
            int count = 0;
            Map<Integer, Node> cache = new HashMap<Integer, Node>();
            Node rootNode = null;
            Statement st = connection.createStatement();
            st.setFetchSize(GET_NODES_FETCH_SIZE);
            
            ResultSet rs = st.executeQuery(
                    String.format(
                            "select id, owner, index, name, node_type " +
                            "from %s " +
                            "order by level"
                        , NODES_TABLE_NAME));
            
            int level = 0;
            while (rs.next())
            {
                if (logger.isDebugEnabled())
                    logger.debug("Creating new node");
                Node node = createNode(rs, cache);
                if (logger.isDebugEnabled())
                    logger.debug(String.format(
                            "Node CREATED: id (%s), name (%s)", node.getId(), node.getName()));
                cache.put(node.getId(), node);
                if (rootNode==null)
                    rootNode = node;
                
                if (node.getLevel()>1 && level!=node.getLevel() )
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Removing unneeded nodes from the cache");
                    level = node.getLevel();
                    int removedCount = 0;
                    Iterator<Node> it = cache.values().iterator();
                    while (it.hasNext())
                    {
                        if (it.next().getLevel()<level-1)
                        {
                            it.remove();
                            ++removedCount;
                        }
                    }
                    if (logger.isDebugEnabled())
                        logger.debug(String.format(
                                "(%s) nodes removed from the cache.", removedCount));
                }
                ++count;
            }
            
            rs.close();
            st.close();
            
            connection.commit();
        
            if (logger.isInfoEnabled())
                logger.info(String.format("Loaded (%s) nodes from the store", count));

            return rootNode;
            
        } catch(Exception e)
        {
            if (logger.isErrorEnabled())
                logger.error("Error reading nodes from H2TreeStore", e);
            throw new TreeStoreError("Error reading nodes", e);
        }
    }
    
    public synchronized void removeNodes() throws TreeStoreError
    {
        try
        {
            Statement st = connection.createStatement();
            st.executeUpdate("delete from "+NODES_TABLE_NAME);

            connection.commit();
            
        } catch(Exception e)
        {
            throw new TreeStoreError("Error while removing all nodes from the store.", e);
        }
    }

    public Connection getConnection()
    {
        return connection;
    }

    private Node createNode(ResultSet rs, Map<Integer, Node> cache) throws Exception
    {
        String nodeType = rs.getString(5);
        if (logger.isDebugEnabled())
            logger.debug(String.format("Creating node of class (%s)", nodeType));
        Node node = (Node) Class.forName(nodeType).newInstance();
        
        node.setId(rs.getInt(1));
        if (logger.isDebugEnabled())
            logger.debug(String.format("Node id is (%s)", node.getId()));
        node.setIndex(rs.getInt(3));
        node.setName(rs.getString(4));
        
        if (cache!=null)
        {
            int parentId = rs.getInt(2);
            if (!rs.wasNull())
            {
                Node parentNode = cache.get(parentId);
                if (parentNode==null)
					logger.error(String.format(
                            "Error adding node with id (%d) to the tree. " +
                            "Parent node (%d) not found."
                            , node.getId(), parentId));
				else
					parentNode.addChildren(node);
            }
        }
                
        if (logger.isDebugEnabled())
            logger.debug(String.format("Creating node attributes", nodeType));
        createNodeAttributes(node);
        
        return node;
    }

    private void createNodeAttributes(Node node) throws Exception
    {
        if (selectNodeAttributesStatement==null)
            selectNodeAttributesStatement = connection.prepareStatement(
                    String.format(
                        "select id, name, display_name, attribute_type, value, required, template_expression, " +
                        "   value_handler_type, parameter_name, parent_attribute, description " +
                        "from %s " +
                        "where owner=?"
                        , NODE_ATTRIBUTES_TABLE_NAME));
        
        selectNodeAttributesStatement.setInt(1, node.getId());
        ResultSet rs = selectNodeAttributesStatement.executeQuery();
        while (rs.next())
        {
            if (logger.isDebugEnabled())
                logger.debug("Creting node attribute");
            int pos=1;
            NodeAttributeImpl attr = new NodeAttributeImpl();
            attr.setId(rs.getInt(pos++));
            attr.setOwner(node);
            attr.setName(rs.getString(pos++));
            attr.setDisplayName(rs.getString(pos++));
            attr.setType(Class.forName(rs.getString(pos++)));
            attr.setRawValue(rs.getString(pos++));
            attr.setRequired(rs.getBoolean(pos++));
            attr.setTemplateExpression(rs.getBoolean(pos++));
            attr.setValueHandlerType(rs.getString(pos++));
            attr.setParameterName(rs.getString(pos++));
            attr.setParentAttribute(rs.getString(pos++));
            if (attr.getParameterName()==null)
                attr.setDescriptionContainer(stringToMessageComposer(rs.getString(pos++)));
            
            node.addNodeAttribute(attr);
            if (logger.isDebugEnabled())
                logger.debug(String.format(
                        "Node attribute (%s) added to the node", attr.getName()));
        }
    }
    
    private void initConnection(String databaseUrl, String username, String password) 
            throws ClassNotFoundException, SQLException
    {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(databaseUrl, username, password);
        connection.setAutoCommit(false);
    }
    
    private void initTables() throws SQLException
    {
        createTables();
    }

    private void insertNode(Node node) throws SQLException
    {
        if (insertNodeStatement == null)
        {
            insertNodeStatement = connection.prepareStatement(
                    String.format(
                    "insert into %s " + 
                    "(level, owner, index, name, node_type) " + 
                    "values (?, ?, ?, ?, ?)"
                    , NODES_TABLE_NAME)
                , Statement.RETURN_GENERATED_KEYS);
        }
        insertNodeStatement.setByte(1, node.getLevel());

        if (node.getParent() == null)
        {
            insertNodeStatement.setNull(2, Types.INTEGER);
        } else
        {
            insertNodeStatement.setInt(2, node.getParent().getId());
        }
        insertNodeStatement.setInt(3, node.getIndex());
        insertNodeStatement.setString(4, node.getName());
        insertNodeStatement.setString(5, node.getClass().getName());

        insertNodeStatement.executeUpdate();
        
        ResultSet rs = insertNodeStatement.getGeneratedKeys();
        rs.next();
        
        node.setId(rs.getInt(1));
        
        rs.close();
    }
    
    private void createTables() throws SQLException
    {
        Statement st = connection.createStatement();
        st.executeUpdate(String.format(
                "create table if not exists %s (" +
                "  id int AUTO_INCREMENT (1)," +
                "  level TINYINT not null," +
                "  owner int," +
                "  index int not null, " +
                "  name varchar(128) not null," +
                "  node_type varchar(256) not null" +
                ")"
                , NODES_TABLE_NAME));
        st.executeUpdate(String.format(
                "create cached table if not exists %s (" +
                "  id int auto_increment (1)," +
                "  owner int," +
                "  name varchar(128)," +
                "  display_name varchar(512)," +
                "  value varchar(10240), " +
                "  attribute_type varchar(256), " +
                "  required boolean, " +
                "  template_expression boolean, " +
                "  value_handler_type varchar(128), " +
                "  parameter_name varchar(128), " +
                "  parent_attribute varchar(128), " +
                "  description varchar(2048), " +
                "  foreign key (owner) references %s (id) on delete cascade" +
                ")"
                , NODE_ATTRIBUTES_TABLE_NAME, NODES_TABLE_NAME));
        st.executeUpdate(String.format(
                "create cached table if not exists %s (" +
                "  id int not null," +
                "  data blob," +
                "  foreign key (id) references %s (id) on delete cascade" +
                ")"
                , NODE_ATTRIBUTES_BINARY_DATA_TABLE_NAME, NODE_ATTRIBUTES_TABLE_NAME));
        st.close();
    }

    private void insertNodeAttribute(NodeAttribute nodeAttribute) throws SQLException
    {
        if (insertNodeAttributeStatement==null)
            insertNodeAttributeStatement = connection.prepareStatement(
                    String.format(
                        "insert into %s " +
                        "(owner, name, display_name, value, attribute_type, required, template_expression" +
                        "  , value_handler_type, parameter_name, parent_attribute, description) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        , NODE_ATTRIBUTES_TABLE_NAME)
                    , Statement.RETURN_GENERATED_KEYS);
        
        int pos=1;
        insertNodeAttributeStatement.setInt(pos++, nodeAttribute.getOwner().getId());
        insertNodeAttributeStatement.setString(pos++, nodeAttribute.getName());
        if (nodeAttribute.getDisplayName()==null)
            insertNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(pos++, nodeAttribute.getDisplayName());
        if (nodeAttribute.getRawValue()==null)
            insertNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(pos++, nodeAttribute.getRawValue());
            
        insertNodeAttributeStatement.setString(pos++, nodeAttribute.getType().getName());
        insertNodeAttributeStatement.setBoolean(pos++, nodeAttribute.isRequired());
        insertNodeAttributeStatement.setBoolean(pos++, nodeAttribute.isTemplateExpression());
        
        if (nodeAttribute.getValueHandlerType()==null)
            insertNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(pos++, nodeAttribute.getValueHandlerType());
        
        if (nodeAttribute.getParameterName()==null)
            insertNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(pos++, nodeAttribute.getParameterName());
        
        if (nodeAttribute.getParentAttribute()==null)
            insertNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(pos++, nodeAttribute.getParentAttribute());
        
        if (nodeAttribute.getParameterName()!=null || nodeAttribute.getDescriptionContainer()==null)
            insertNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(
                    pos++, messageComposerToString(nodeAttribute.getDescriptionContainer()));
        
        insertNodeAttributeStatement.executeUpdate();
        ResultSet rs = insertNodeAttributeStatement.getGeneratedKeys();
        rs.next();
        nodeAttribute.setId(rs.getInt(1));
        
        rs.close();
    }

    private void updateNode(Node node) throws SQLException
    {
        if (updateNodeStatement==null)
            updateNodeStatement = connection.prepareStatement(
                    String.format(
                        "update %s " +
                        "set level=?, owner=?, index=?, name=?, node_type=? " +
                        "where id=? "
                        , NODES_TABLE_NAME));
        updateNodeStatement.setByte(1, node.getLevel());
        
        if (node.getParent()==null)
            updateNodeStatement.setNull(2, Types.INTEGER);
        else
            updateNodeStatement.setInt(2, node.getParent().getId());
        updateNodeStatement.setInt(3, node.getIndex());
        updateNodeStatement.setString(4, node.getName());
        updateNodeStatement.setString(5, node.getClass().getName());
        
        updateNodeStatement.setInt(6, node.getId());
        
        updateNodeStatement.executeUpdate();
    }

    private void updateNodeAttribute(NodeAttribute nodeAttribute) throws SQLException
    {
        if (updateNodeAttributeStatement==null)
            updateNodeAttributeStatement = connection.prepareStatement(
                    String.format(
                        "update %s " +
                        "set owner=?, name=?, display_name=?, value=?, attribute_type=?, required=?, " +
                        "   template_expression=?, value_handler_type=?, parameter_name=?, " +
                        "   parent_attribute=?, description=? " +
                        "where id=?"
                        , NODE_ATTRIBUTES_TABLE_NAME));
        
        int pos=1;
        updateNodeAttributeStatement.setInt(pos++, nodeAttribute.getOwner().getId());
        updateNodeAttributeStatement.setString(pos++, nodeAttribute.getName());
        
        if (nodeAttribute.getDisplayName()==null)
            updateNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(pos++, nodeAttribute.getDisplayName());
        
        if (nodeAttribute.getRawValue()==null)
            updateNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(pos++, nodeAttribute.getRawValue());
        
        if (nodeAttribute.getType().getName()==null)
            updateNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(pos++, nodeAttribute.getType().getName());
        
        updateNodeAttributeStatement.setBoolean(pos++, nodeAttribute.isRequired());
        
        updateNodeAttributeStatement.setBoolean(pos++, nodeAttribute.isTemplateExpression());
        
        if (nodeAttribute.getValueHandlerType()==null)
            updateNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(pos++, nodeAttribute.getValueHandlerType());
        
        if (nodeAttribute.getParameterName()==null)
            updateNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(pos++, nodeAttribute.getParameterName());
        
        if (nodeAttribute.getParentAttribute()==null)
            updateNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(pos++, nodeAttribute.getParentAttribute());
        
        if (nodeAttribute.getParameterName()!=null || nodeAttribute.getDescription()==null)
            updateNodeAttributeStatement.setNull(pos++, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(
                    pos++, messageComposerToString(nodeAttribute.getDescriptionContainer()));
        
        updateNodeAttributeStatement.setInt(pos++, nodeAttribute.getId());
        
        updateNodeAttributeStatement.executeUpdate();
    }

    private static String messageComposerToString(MessageComposer composer)
    {
        if (composer==null)
            return null;

        StringBuilder builder = new StringBuilder();
        for (String message: composer.getMessages())
        {
            if (builder.length()>0)
                builder.append(',');
            builder.append(StringEscapeUtils.escapeCsv(message));
        }
        return builder.toString();
    }

    private static MessageComposer stringToMessageComposer(String str)
    {
        if (str==null)
            return null;

        MessageComposer composer = new MessageComposer(messagesRegistry);
        StrTokenizer tokenizer = new StrTokenizer(str, ',', '"');
        while (tokenizer.hasNext())
            composer.append(tokenizer.nextToken());

        return composer;
    }

    public void close() throws TreeStoreError 
    {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e)
        {
            throw new TreeStoreError("Close store error", e);
        }
    }
}
