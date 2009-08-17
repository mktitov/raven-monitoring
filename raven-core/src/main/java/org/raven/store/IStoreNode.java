package org.raven.store;

import org.raven.dbcp.ConnectionPool;
import org.raven.tree.Node;

public interface IStoreNode extends Node {

	public ConnectionPool getConnectionPool();
	
}
