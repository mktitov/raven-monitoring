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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.store.TreeStore;
import org.raven.tree.store.TreeStoreError;

/**
 *
 * @author Mikhail Titov
 */
public class H2TreeStore implements TreeStore
{
    public final static String NODES_TABLE_NAME = "NODES" ;
    public final static String NODE_ATTRIBUTES_TABLE_NAME = "NODE_ATTRIBUTES";
    
    private Connection connection; 

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

    public void saveNode(Node node)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void saveNodeAttribute(NodeAttribute nodeAttribute)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Iterator<Node> getNodes()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void initConnection(String databaseUrl, String username, String password) 
            throws ClassNotFoundException, SQLException
    {
        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection(databaseUrl, username, password);
    }
    
    private void initTables() throws SQLException
    {
        createNodesTable();
    }
    
    private boolean isTableExists(String tableName) throws SQLException
    {
        Statement st = connection.createStatement();
        ResultSet rs = 
                st.executeQuery(String.format(
                    "select count(*) from information_schema.tables where table_name = '%s'"
                    , tableName));
        try
        {
            rs.next();
            return rs.getInt(1) == 1;
        } finally 
        {
            rs.close();
            st.close();
        }
    }

    private void createTables() throws SQLException
    {
        Statement st = connection.createStatement();
        st.executeUpdate(String.format(
                "create table if not exists %s (" +
                "  id int AUTO_INCREMENT," +
                "  level TINYINT not null," +
                "  owner int," +
                "  name varchar(128)," +
                "  node_type varchar(256)," +
                "  node_logic_type varchar(256)" +
                ")"
                , NODES_TABLE_NAME));
        st.executeUpdate(String.format(
                "create cached table if not exists node_attributes (" +
                "  id int auto_increment," +
                "  owner int," +
                "  foreign key (owner) references nodes (id) on delete cascade" +
                ")"
                ));
        st.close();
    }
}
