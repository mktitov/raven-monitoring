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
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.store.TreeStore;
import org.raven.tree.store.TreeStoreError;

/**
 *
 * @author Mikhail Titov
 */
public class H2TreeStore implements TreeStore
{
    public static final int GET_NODES_FETCH_SIZE = 1000;
    public final static String NODES_TABLE_NAME = "NODES" ;
    public final static String NODE_ATTRIBUTES_TABLE_NAME = "NODE_ATTRIBUTES";
    
    private Connection connection; 
    
    private PreparedStatement insertNodeStatement;
    private PreparedStatement selectNodeStatement;
    private PreparedStatement updateNodeStatement;
    private PreparedStatement removeNodeStatement;
    private PreparedStatement insertNodeAttributeStatement;
    private PreparedStatement updateNodeAttributeStatement;
    private PreparedStatement removeNodeAttributeStatement;
    private PreparedStatement selectNodeAttributesStatement;

    public void init(String databaseUrl, String username, String password) throws TreeStoreError
    {
        try
        {
            initConnection(databaseUrl, username, password);
            initTables();
        } catch (Exception ex)
        {
            throw new TreeStoreError("H2 Tree store initialization error", ex);
        }
    }

    public void saveNode(Node node) throws TreeStoreError
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

    public Node getNode(int id) throws TreeStoreError
    {
        try
        {
            if (selectNodeStatement == null)
            {
                selectNodeStatement = connection.prepareStatement(
                        String.format(
                            "select id, owner, name, node_type, node_logic_type " +
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
            }
            
        } catch (Exception e)
        {
            throw new TreeStoreError(
                    String.format("Error getting node by id (%d)", id)
                    , e);
        }
    }

    public void removeNode(int id) throws TreeStoreError
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

    public void saveNodeAttribute(NodeAttribute nodeAttribute) throws TreeStoreError
    {
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

    public void removeNodeAttribute(int id) throws TreeStoreError
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

//    public NodeAttribute getNodeAttribute(int id) throws TreeStoreError
//    {
//        try
//        {
//            if (selectNodeAttributeStatement==null)
//                selectNodeAttributeStatement = connection.prepareStatement(
//                        String.format(                        
//                            "select id, owner, name, attribute_type, value, parameter_name" +
//                            "   , parent_attribute, description"));
//        } catch (Exception e)
//        {
//            throw new TreeStoreError(
//                    String.format(
//                        "Error loading node attribute with id (%d)", id)
//                    , e);
//        }
//    }

    public Node getRootNode() throws TreeStoreError
    {
        try
        {
            Map<Integer, Node> cache = new HashMap<Integer, Node>();
            Node rootNode = null;
            Statement st = connection.createStatement();
            st.setFetchSize(GET_NODES_FETCH_SIZE);
            
            ResultSet rs = st.executeQuery(
                    String.format(
                            "select id, owner, name, node_type, node_logic_type " +
                            "from %s " +
                            "order by level"
                        , NODES_TABLE_NAME));
            
            int level = 0;
            while (rs.next())
            {
                Node node = createNode(rs, cache);
                cache.put(node.getId(), node);
                if (rootNode==null)
                    rootNode = node;
                
                if (node.getLevel()>1 && level!=node.getLevel() )
                {
                    level = node.getLevel();
                    Iterator<Node> it = cache.values().iterator();
                    while (it.hasNext())
                    {
                        if (it.next().getLevel()<level-1)
                            it.remove();
                    }
                }
            }
            
            rs.close();
            st.close();
        
            return rootNode;
            
        } catch(Exception e)
        {
            throw new TreeStoreError("Error reading nodes", e);
        }
    }
    
    public void deleteNodes() throws SQLException
    {
        Statement st = connection.createStatement();
        st.executeUpdate("delete from "+NODES_TABLE_NAME);
        
        connection.commit();
    }

    private Node createNode(ResultSet rs, Map<Integer, Node> cache) throws Exception
    {
        String nodeType = rs.getString(4);
        Node node = (Node) Class.forName(nodeType).newInstance();
        
        node.setId(rs.getInt(1));
        if (cache!=null)
        {
            int parentId = rs.getInt(2);
            if (!rs.wasNull())
            {
                Node parentNode = cache.get(parentId);
                parentNode.addChildren(node);
            }
        }
        
        node.setName(rs.getString(3));
        
        String nodeLogicTypeName = rs.getString(5);
        if (!rs.wasNull())
            node.setNodeLogicType(Class.forName(nodeLogicTypeName));
        
        createNodeAttributes(node);
        
        return node;
    }

    private void createNodeAttributes(Node node) throws Exception
    {
        if (selectNodeAttributesStatement==null)
            selectNodeAttributesStatement = connection.prepareStatement(
                    String.format(
                        "select id, name, attribute_type, value, parameter_name" +
                        "   , parent_attribute, description " +
                        "from %s " +
                        "where owner=?"
                        , NODE_ATTRIBUTES_TABLE_NAME));
        
        selectNodeAttributesStatement.setInt(1, node.getId());
        ResultSet rs = selectNodeAttributesStatement.executeQuery();
        while (rs.next())
        {
            NodeAttributeImpl attr = new NodeAttributeImpl();
            attr.setId(rs.getInt(1));
            attr.setOwner(node);
            attr.setName(rs.getString(2));
            attr.setType(Class.forName(rs.getString(3)));
            attr.setValue(rs.getString(4));
            attr.setParameterName(rs.getString(5));
            attr.setParentAttribute(rs.getString(6));
            attr.setDescription(rs.getString(7));
            
            node.addNodeAttribute(attr);
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
                    "(level, owner, name, node_type, node_logic_type) " + 
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
        insertNodeStatement.setString(3, node.getName());
        insertNodeStatement.setString(4, node.getClass().getName());

        if (node.getNodeLogicType() == null)
        {
            insertNodeStatement.setNull(5, Types.VARCHAR);
        } else
        {
            insertNodeStatement.setString(5, node.getNodeLogicType().getName());
        }
        
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
                "  name varchar(128)," +
                "  node_type varchar(256)," +
                "  node_logic_type varchar(256)" +
                ")"
                , NODES_TABLE_NAME));
        st.executeUpdate(String.format(
                "create cached table if not exists %s (" +
                "  id int auto_increment (1)," +
                "  owner int," +
                "  name varchar(128)," +
                "  value varchar(256), " +
                "  attribute_type varchar(256)," +
                "  parameter_name varchar(128)," +
                "  parent_attribute varchar(128)," +
                "  description varchar(256)," +
                "  foreign key (owner) references %s (id) on delete cascade" +
                ")"
                , NODE_ATTRIBUTES_TABLE_NAME, NODES_TABLE_NAME));
        st.close();
    }

    private void insertNodeAttribute(NodeAttribute nodeAttribute) throws SQLException
    {
        if (insertNodeAttributeStatement==null)
            insertNodeAttributeStatement = connection.prepareStatement(
                    String.format(
                        "insert into %s " +
                        "(owner, name, value, attribute_type, parameter_name, " +
                        " parent_attribute, description) " +
                        "values (?, ?, ?, ?, ?, ?, ?)"
                        , NODE_ATTRIBUTES_TABLE_NAME)
                    , Statement.RETURN_GENERATED_KEYS);
        
        insertNodeAttributeStatement.setInt(1, nodeAttribute.getOwner().getId());
        insertNodeAttributeStatement.setString(2, nodeAttribute.getName());
        
        if (nodeAttribute.getValue()==null)
            insertNodeAttributeStatement.setNull(3, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(3, nodeAttribute.getValue());
            
        insertNodeAttributeStatement.setString(4, nodeAttribute.getType().getName());
        
        if (nodeAttribute.getParameterName()==null)
            insertNodeAttributeStatement.setNull(5, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(5, nodeAttribute.getParameterName());
        
        if (nodeAttribute.getParentAttribute()==null)
            insertNodeAttributeStatement.setNull(6, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(6, nodeAttribute.getParentAttribute());
        
        if (nodeAttribute.getDescription()==null)
            insertNodeAttributeStatement.setNull(7, Types.VARCHAR);
        else
            insertNodeAttributeStatement.setString(7, nodeAttribute.getDescription());
        
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
                        "set level=?, owner=?, name=?, node_type=?, node_logic_type=? " +
                        "where id=? "
                        , NODES_TABLE_NAME));
        updateNodeStatement.setByte(1, node.getLevel());
        
        if (node.getParent()==null)
            updateNodeStatement.setNull(2, Types.INTEGER);
        else
            updateNodeStatement.setInt(2, node.getParent().getId());
        
        updateNodeStatement.setString(3, node.getName());
        updateNodeStatement.setString(4, node.getClass().getName());
        
        if (node.getNodeLogicType()==null)
            updateNodeStatement.setNull(5, Types.VARCHAR);
        else
            updateNodeStatement.setString(5, node.getNodeLogicType().getName());
        
        updateNodeStatement.setInt(6, node.getId());
        
        updateNodeStatement.executeUpdate();
    }

    private void updateNodeAttribute(NodeAttribute nodeAttribute) throws SQLException
    {
        if (updateNodeAttributeStatement==null)
            updateNodeAttributeStatement = connection.prepareStatement(
                    String.format(
                        "update %s " +
                        "set owner=?, name=?, value=?, attribute_type=?, parameter_name=?, " +
                        "   parent_attribute=?, description=? " +
                        "where id=?"
                        , NODE_ATTRIBUTES_TABLE_NAME));
        
        updateNodeAttributeStatement.setInt(1, nodeAttribute.getOwner().getId());
        updateNodeAttributeStatement.setString(2, nodeAttribute.getName());
        
        if (nodeAttribute.getValue()==null)
            updateNodeAttributeStatement.setNull(3, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(3, nodeAttribute.getValue());
        
        if (nodeAttribute.getType().getName()==null)
            updateNodeAttributeStatement.setNull(4, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(4, nodeAttribute.getType().getName());
        
        if (nodeAttribute.getParameterName()==null)
            updateNodeAttributeStatement.setNull(5, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(5, nodeAttribute.getParameterName());
        
        if (nodeAttribute.getParentAttribute()==null)
            updateNodeAttributeStatement.setNull(6, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(6, nodeAttribute.getParentAttribute());
        
        if (nodeAttribute.getDescription()==null)
            updateNodeAttributeStatement.setNull(7, Types.VARCHAR);
        else
            updateNodeAttributeStatement.setString(7, nodeAttribute.getDescription());
        
        updateNodeAttributeStatement.setInt(8, nodeAttribute.getId());
        
        updateNodeAttributeStatement.executeUpdate();
    }
}
