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

package org.raven.rep;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.util.Map;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.Record;
import org.raven.ds.impl.BinaryFieldValue;
import org.raven.ds.impl.ConnectionPoolValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SqlJasperReport extends AbstractJasperReport
{
    public static final int INITIAL_REPORT_BUFFER_SIZE = 65536;
    
    @Parameter(valueHandlerType=ConnectionPoolValueHandlerFactory.TYPE)
    @NotNull
    private ConnectionPool connectionPool;

    public ConnectionPool getConnectionPool()
    {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool)
    {
        this.connectionPool = connectionPool;
    }

    @Override
    public void generateReport(Record record) throws Exception
    {
        Map<String, Object> params = getReportParameters().getParameterValues();
        Connection connection = connectionPool.getConnection();
        try
        {
            JasperPrint reportData = JasperFillManager.fillReport(
                    getReportFile().getDataStream(), params, connection);
            ByteArrayOutputStream buf = new ByteArrayOutputStream(INITIAL_REPORT_BUFFER_SIZE);
            ObjectOutputStream os = new ObjectOutputStream(buf);
            os.writeObject(reportData);
            os.close();

            byte[] reportBytes = buf.toByteArray();
            buf.close();

            BinaryFieldValue value = new BinaryFieldValue(reportBytes);
            record.setValue(ReportRecordSchemaNode.REPORTDATA_FIELD_NAME, value);
        }
        finally
        {
            connection.close();
        }
    }
}
