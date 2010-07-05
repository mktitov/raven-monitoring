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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.expr.BindingSupport;
import org.raven.tree.DataFile;
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
public class JxlsReportNode extends AbstractSafeDataPipe implements Viewable
{
    @NotNull @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    private DataFile reportTemplate;

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
        try {
            Map b = beans.get();
            b.put("data", data);
            XLSTransformer transformer = new XLSTransformer();
            File tempFile = File.createTempFile("jxls_"+getId()+"_", ".xls");
            try{
                HSSFWorkbook wb = transformer.transformXLS(reportTemplate.getDataStream(), b);
                FileOutputStream fos = new FileOutputStream(tempFile);
                try{
                    wb.write(fos);
                }finally{
                    fos.close();
                }
                FileInputStream is = new FileInputStream(tempFile);
                try{
                    sendDataToConsumers(is, context);
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
}
