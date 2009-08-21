package org.raven.store;

import org.raven.dbcp.ConnectionPool;
import org.raven.tree.Node;

public interface INodeHasPool extends Node 
{
    public ConnectionPool getConnectionPool();

    public void setConnectionPool(ConnectionPool connectionPool);
}
