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
import java.util.Properties;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.log.LogLevel;
import org.raven.tree.NodeError;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=ConnectionPoolsNode.class)
public class JDBCConnectionPoolNode extends BaseNode implements ConnectionPool
{
    @Parameter @NotNull 
    private String userName;
    @Parameter
    private String password;
    @Parameter @NotNull
    private String url;
    @Parameter @NotNull
    private String driver;
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
    @NotNull @Parameter(defaultValue="60000")
    private Long minIdleTime;
    @Parameter
    private String validationQuery;
    @Parameter
    private String connectionProperties;

    private GenericObjectPool connectionPool;
	private PoolingDriver poolingDriver;

    @Override
    protected void initFields() {
        super.initFields();
        connectionPool = null;
        poolingDriver = null;
    }
    
    private String getPoolName() {
        return "connectionPool_"+getId();
    }

    @Override
    protected void doStart() throws Exception {
        connectionPool = new GenericObjectPool(null);
        connectionPool.setMaxActive(maxActive);
        connectionPool.setMaxIdle(maxIdle);
        connectionPool.setMaxWait(maxWait);
        connectionPool.setTimeBetweenEvictionRunsMillis(30000);
        connectionPool.setMinEvictableIdleTimeMillis(minIdleTime);
        connectionPool.setTestWhileIdle(true);
        
        Properties props = new Properties();
        props.setProperty("user", userName);
        String _pass = password;
        props.setProperty("password", _pass==null?"":_pass);
        String _connectionProperties = connectionProperties;
        if (_connectionProperties!=null) {
            String[] lines = _connectionProperties.split("\\s*;\\s*");
            if (lines!=null)
                for (String line: lines) {
                    String[] prop = line.split("\\s*=\\s*");
                    if (prop!=null && prop.length==2)
                        props.setProperty(prop[0], prop[1]);
                    else 
                        throw new Exception("Invalid connection property: "+line);
                }
        }
        ConnectionFactory connectionFactory =
                new DriverManagerConnectionFactory(url, props);
        PoolableConnectionFactory poolableConnectionFactory =
                new PoolableConnectionFactory(
                    connectionFactory, connectionPool, null, validationQuery, false, autoCommit);
        Class.forName("org.apache.commons.dbcp.PoolingDriver");
        Class.forName(driver);
        poolingDriver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        poolingDriver.registerPool(getPoolName(), connectionPool);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        poolingDriver.closePool(getPoolName());
    }
       
    public Connection getConnection() throws SQLException {
        if (!isStarted()) {
            final String mess = "Can't get connection because of connection pool not started";
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(mess);
            throw new SQLException(mess);
        } else {
            try {
                return DriverManager.getConnection("jdbc:apache:commons:dbcp:"+getPoolName());
            } catch (SQLException ex) {
                String message = String.format(
                        "Error creating connection in connection pool (%s)", getPath());
                logger.error(message, ex);
                throw new NodeError(message, ex);
            }
        }
    }

    public String getConnectionProperties() {
        return connectionProperties;
    }

    public void setConnectionProperties(String connectionProperties) {
        this.connectionProperties = connectionProperties;
    }
    
    @Parameter(readOnly=true)
    public Integer getNumberOfActiveConnections() {
        GenericObjectPool _connectionPool = connectionPool;
        return _connectionPool!=null? _connectionPool.getNumActive() : null;
    }
    
    @Parameter(readOnly=true)
    public Integer getNumberOfIdleConnections() {
        GenericObjectPool _connectionPool = connectionPool;
        return _connectionPool!=null? _connectionPool.getNumIdle() : null;
    }
    
    @Parameter(readOnly=true)
    public Integer getNumberOfTestsPerEvictionRun() {
        GenericObjectPool _connectionPool = connectionPool;
        return _connectionPool!=null? _connectionPool.getNumTestsPerEvictionRun() : null;
    }

    public Long getMinIdleTime() {
        return minIdleTime;
    }

    public void setMinIdleTime(Long minIdleTime) {
        this.minIdleTime = minIdleTime;
    }

    public Boolean getAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(Boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public String getDefaultCatalog() {
        return defaultCatalog;
    }

    public void setDefaultCatalog(String defaultCatalog) {
        this.defaultCatalog = defaultCatalog;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public Integer getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(Integer initialSize) {
        this.initialSize = initialSize;
    }

    public Integer getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(Integer maxActive) {
        this.maxActive = maxActive;
    }

    public Integer getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(Integer maxIdle) {
        this.maxIdle = maxIdle;
    }

    public Integer getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(Integer maxWait) {
        this.maxWait = maxWait;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

}
