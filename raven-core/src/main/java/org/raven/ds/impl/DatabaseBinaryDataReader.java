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

package org.raven.ds.impl;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.BinaryFieldType;
import org.raven.ds.BinaryFieldTypeException;

/**
 *
 * @author Mikhail Titov
 */
public class DatabaseBinaryDataReader implements BinaryFieldType
{
    private final String tableName;
    private final String idColumnName;
    private final String binaryColumnName;
    private final Object idColumnValue;
    private final ConnectionPool connectionPool;

    public DatabaseBinaryDataReader(
            String tableName, String idColumnName, String binaryColumnName, Object idColumnValue
            , ConnectionPool connectionPool)
    {
        this.tableName = tableName;
        this.idColumnName = idColumnName;
        this.binaryColumnName = binaryColumnName;
        this.idColumnValue = idColumnValue;
        this.connectionPool = connectionPool;
    }

    public InputStream getData() throws BinaryFieldTypeException
    {
        try
        {
            String query = String.format(
                    "select %s from %s where %s=?", binaryColumnName, tableName, idColumnName);
            Connection connection = connectionPool.getConnection();
            try
            {
                PreparedStatement st = null;
                ResultSet rs = null;
                try
                {
                    st = connection.prepareStatement(query);
                    st.setObject(1, idColumnValue);
                    rs = st.executeQuery();
                    if (rs.next())
                        return rs.getBinaryStream(1);
                    else
                        return null;
                }
                finally
                {
                    if (rs!=null)
                        rs.close();
                    if (st!=null)
                        st.close();
                }
            }
            finally
            {
                connection.close();
            }
        }
        catch(Exception e)
        {
            throw new BinaryFieldTypeException(e);
        }
    }

    public void closeResources() throws BinaryFieldTypeException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
