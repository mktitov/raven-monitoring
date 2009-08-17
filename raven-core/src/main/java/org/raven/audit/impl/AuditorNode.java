package org.raven.audit.impl;

import org.raven.annotations.Parameter;
import org.raven.audit.Auditor;
import org.raven.dbcp.ConnectionPool;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;
import org.raven.tree.impl.BaseNode;

public class AuditorNode extends BaseNode
{
	    public static String NAME = "Auditor";

	    @Service
	    private static Auditor auditor;

	    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
	    @NotNull
	    private ConnectionPool connectionPool;

	    public AuditorNode()
	    {
	        super(NAME);
	    }

	    @Override
	    protected void doStart() throws Exception
	    {
	        super.doStart();
	        auditor.setAuditorNode(this);
	    }

	    @Override
	    protected void doStop() throws Exception
	    {
	        super.doStop();
	        auditor.setAuditorNode(null);
	    }

	    public ConnectionPool getConnectionPool()
	    {
	        return connectionPool;
	    }

	    public void setConnectionPool(ConnectionPool connectionPool)
	    {
	        this.connectionPool = connectionPool;
	    }

	    @Override
	    protected boolean includeLogLevel()
	    {
	        return false;
	    }
	
}
