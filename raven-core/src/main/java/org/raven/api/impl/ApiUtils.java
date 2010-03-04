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

package org.raven.api.impl;

import groovy.lang.Closure;
import groovy.sql.Sql;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author Mikhail Titov
 */
public class ApiUtils
{
    public static void withConnection(Connection connection, Closure closure) throws SQLException
    {
        try
        {
            closure.call(connection);
        }
        finally
        {
            connection.close();
        }
    }

    public static void withSql(Connection connection, Closure closure) throws Exception
    {
        try
        {
            Sql sql = new Sql(connection);
            try{
                closure.call(sql);
                connection.commit();
            }catch(Exception e){
                connection.rollback();
                throw e;
            }
        }
        finally
        {
            connection.close();
        }
    }
}
