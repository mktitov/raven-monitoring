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

package org.raven.dbcp.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.tree.NodeError;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ConnectPoolNode extends BaseNode implements ConnectionPool
{
    @Parameter @NotNull 
    private String userName;
    @Parameter
    private String password;
    @Parameter @NotNull
    private String url;
    @Parameter @NotNull
    private String driverClass;
    @Parameter (defaultValue="true") @NotNull
    private Boolean autoCommit;
    @Parameter
    private String defaultCatalog;
    @Parameter(defaultValue="0") @NotNull
    private Integer initialSize;
    @Parameter(defaultValue="8") @NotNull
    private Integer maxActive;
    @Parameter(defaultValue="2") @NotNull
    private Integer maxIdle;
    @Parameter(defaultValue="10000") @NotNull
    private Integer maxWait;
    @Parameter
    private String validationQuery;

    private GenericObjectPool connectionPool;


    @Override
    protected void doStart() throws Exception
    {
        connectionPool = new GenericObjectPool(null);
        connectionPool.setMaxActive(maxActive);
        connectionPool.setMaxIdle(maxIdle);
        connectionPool.setMaxWait(maxWait);
        
        ConnectionFactory connectionFactory =
                new DriverManagerConnectionFactory(url, userName, password);
        PoolableConnectionFactory poolableConnectionFactory =
                new PoolableConnectionFactory(
                    connectionFactory, connectionPool, null, validationQuery, false, autoCommit);
        Class.forName("org.apache.commons.dbcp.PoolingDriver");
        PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        driver.registerPool(getName(), connectionPool);
    }
    
    public Connection getConnection() 
    {
        if (getStatus()!=Status.STARTED)
            return null;
        else
        {
            try {
                return DriverManager.getConnection("jdbc:apache:commons:dbcp:"+getName());
            }
            catch (SQLException ex)
            {
                String message = String.format(
                        "Error creating connection in connection pool (%s)", getPath());
                logger.error(message, ex);
                throw new NodeError(message, ex);
            }
        }
    }

    public Boolean getAutoCommit() {
        return autoCommit;
    }

    public String getDefaultCatalog() {
        return defaultCatalog;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public Integer getInitialSize() {
        return initialSize;
    }

    public Integer getMaxActive() {
        return maxActive;
    }

    public Integer getMaxIdle() {
        return maxIdle;
    }

    public Integer getMaxWait() {
        return maxWait;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public String getValidationQuery() {
        return validationQuery;
    }
    
}
