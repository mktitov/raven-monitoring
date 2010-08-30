/*
 *  Copyright 2010 Mikhail Titov.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.activation.FileDataSource;
import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.FieldValueGenerator;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.ds.impl.AttributeFieldValueGenerator;
import org.raven.ds.impl.DataSourceFieldValueGenerator;
import org.raven.ds.impl.InputStreamBinaryFieldValue;
import org.raven.expr.BindingSupport;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.DataFileViewableObject;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes={AttributeFieldValueGenerator.class, DataSourceFieldValueGenerator.class})
public class JxlsReportNode extends AbstractSafeDataPipe implements Viewable
{
    @NotNull @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    private DataFile reportTemplate;

    @Parameter
    private String reportFieldName;

    private ThreadLocal<Map> beans;

    @Override
    protected void initFields()
    {
        super.initFields();
        beans = new ThreadLocal<Map>(){
            @Override
            protected Map initialValue() {
                return new HashMap();
            }
        };
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
        bindingSupport.put("beans", beans.get());
    }
    
    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (data==null){
            sendDataToConsumers(null, context);
            return;
        }
        try {
            Map b = beans.get();
            Collection<Node> childs = getChildrens();
            if (childs!=null)
            {
                bindingSupport.put(DATA_BINDING, data);
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                try{
                    for (Node child: childs)
                        if (child instanceof FieldValueGenerator)
                            b.put(child.getName(), 
                                    ((FieldValueGenerator)child).getFieldValue(context.getSessionAttributes()));
                }finally{
                    bindingSupport.reset();
                }
            }
                        
            b.put("data", data);
            XLSTransformer transformer = new XLSTransformer();
            File tempFile = File.createTempFile("jxls_"+getId()+"_", ".xls");
            FileDataSource ds = new FileDataSource(tempFile);
            try{
                HSSFWorkbook wb = transformer.transformXLS(reportTemplate.getDataStream(), b);
//                FileOutputStream fos = new FileOutputStream(tempFile);
                OutputStream os = ds.getOutputStream();
                try{
                    wb.write(os);
                }finally{
                    os.close();
                }
//                FileInputStream is = new FileInputStream(tempFile);
                InputStream is = ds.getInputStream();
                try{
                    Object res = ds;
                    String _reportFieldName = reportFieldName;
                    if (_reportFieldName!=null && (data instanceof Record)){
                        Record rec = (Record) data;
                        RecordSchemaField field = RavenUtils.getRecordSchemaField(rec.getSchema(), _reportFieldName);
                        if (field!=null && field.getFieldType().equals(RecordSchemaFieldType.BINARY)) {
                            rec.setValue(_reportFieldName, new InputStreamBinaryFieldValue(is));
                            res = rec;
                        }
                    }
                    sendDataToConsumers(res, context);
                }finally{
                    is.close();
                }
            }finally{
                tempFile.delete();
            }
        } finally {
            beans.remove();
        }
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;

        return Arrays.asList((ViewableObject)new DataFileViewableObject(reportTemplate, this));
    }

    public Boolean getAutoRefresh() {
        return true;
    }

    public DataFile getReportTemplate() {
        return reportTemplate;
    }

    public void setReportTemplate(DataFile reportTemplate) {
        this.reportTemplate = reportTemplate;
    }

    public String getReportFieldName() {
        return reportFieldName;
    }

    public void setReportFieldName(String reportFieldName) {
        this.reportFieldName = reportFieldName;
    }
}
