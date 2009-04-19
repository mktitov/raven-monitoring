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

import java.util.Collection;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.DataFile;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.ParametersNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractJasperReport extends AbstractDataSource implements Schedulable
{
    @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    @NotNull
    private DataFile reportFile;

    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    @NotNull
    private Scheduler scheduler;

    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter @NotNull
    private String reportType;

    @Parameter @NotNull
    private String reportName;

    private ParametersNode reportParameters;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();

        createReportParametersNode();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        createReportParametersNode();
    }

    public ParametersNode getReportParameters()
    {
        return reportParameters;
    }

    public DataFile getReportFile()
    {
        return reportFile;
    }

    public void setReportFile(DataFile reportFile)
    {
        this.reportFile = reportFile;
    }

    public Scheduler getScheduler()
    {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public String getReportName()
    {
        return reportName;
    }

    public void setReportName(String reportName)
    {
        this.reportName = reportName;
    }

    public String getReportType()
    {
        return reportType;
    }

    public void setReportType(String reportType)
    {
        this.reportType = reportType;
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        Record record = prepareRecord();
        generateReport(record);
        dataConsumer.setData(this, record);
        dataConsumer.setData(this, null);
        
        return true;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    public void executeScheduledJob()
    {
        try
        {
            Record record = prepareRecord();
            generateReport(record);
            sendDataToConsumers(record);
            sendDataToConsumers(null);
        }
        catch (Exception ex)
        {
            error("Error generating report", ex);
        }
    }

    private Record prepareRecord() throws RecordException
    {
        Record record = recordSchema.createRecord();
        record.setValue(ReportRecordSchemaNode.TYPE_FIELD_NAME, reportType);
        record.setValue(ReportRecordSchemaNode.NAME_FIELD_NAME, reportName);
        record.setValue(ReportRecordSchemaNode.GENERATIONDATE_FIELD_NAME
                , new java.sql.Timestamp(System.currentTimeMillis()));
        return record;
    }

    private void createReportParametersNode()
    {
        reportParameters = (ParametersNode) getChildren(ParametersNode.NAME);
        if (reportParameters==null)
        {
            reportParameters = new ParametersNode();
            this.addAndSaveChildren(reportParameters);
            reportParameters.start();
        }
    }

    public abstract void generateReport(Record record) throws Exception;
}
