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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.io.IOUtils;
import org.h2.Driver;
import org.junit.Assert;
import org.junit.Test;
import org.raven.dbcp.ConnectionPool;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class DatabaseBinaryDataReaderTest extends Assert
{
    @Test
    public void test() throws Exception
    {
        Class.forName(Driver.class.getName());
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:tcp://localhost/~/test;", "sa", "");
        initData(connection);

        ConnectionPool pool = createMock(ConnectionPool.class);
        expect(pool.getConnection()).andReturn(connection);
        replay(pool);

        DatabaseBinaryDataReader reader =
                new DatabaseBinaryDataReader("binary_data", "id", "data", 2, pool);
        InputStream is = reader.getData();
        assertNotNull(is);
        assertArrayEquals(new byte[]{1,2,3}, IOUtils.toByteArray(is));
        assertTrue(connection.isClosed());
    }

    private void initData(Connection connection) throws SQLException
    {
        Statement st = connection.createStatement();
        st.executeUpdate("drop table if exists binary_data");
        st.executeUpdate("create table binary_data (id int, data blob)");
        st.executeUpdate("insert into binary_data(id) values(1)");

        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1,2,3});
        PreparedStatement pst = connection.prepareStatement(
                "insert into binary_data (id, data) values (?, ?)");
        pst.setInt(1, 2);
        pst.setBinaryStream(2, is);
        pst.executeUpdate();
        connection.commit();
    }
}