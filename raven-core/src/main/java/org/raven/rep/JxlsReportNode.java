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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.activation.FileDataSource;
import net.sf.jxls.transformer.XLSTransformer;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.ds.impl.InputStreamBinaryFieldValue;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.tree.DataFile;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.DataFileViewableObject;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(
    childNodes={
        JxlsAttributeValueBeanNode.class, JxlsDataSourceBeanNode.class,
        CellStyleSelectorNode.class})
public class JxlsReportNode extends AbstractSafeDataPipe implements Viewable
{
    public static final String BEANS_BINDING = "beans";
    public final static String SHEET_NAME_ATTR = "sheetName";

    @NotNull @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    private DataFile reportTemplate;

    @Parameter
    private String reportFieldName;

    @NotNull @Parameter(defaultValue="false")
    private Boolean multiSheetReport;

    @NotNull @Parameter(defaultValue="Report")
    private String sheetName;

    @NotNull @Parameter(defaultValue="Template")
    private String templateSheetName;

    private ThreadLocal<List<SheetInfo>> sheetsStore;
    private ThreadLocal<Map> beansStore;

    @Override
    protected void initFields()
    {
        super.initFields();
        sheetsStore = new ThreadLocal<List<SheetInfo>>();
        beansStore = new ThreadLocal<Map>(){
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
        bindingSupport.put(BEANS_BINDING, beansStore.get());
    }
    
    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (data==null){
            generateReport(context);
            sendDataToConsumers(null, context);
            return;
        }
        Map beans = new HashMap();
        try{
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            bindingSupport.put(BEANS_BINDING, beansStore.get());
            Set<String> fixedSizeCollectionBeanNames = new HashSet<String>();
            for (JxlsBean bean: NodeUtils.getChildsOfType(this, JxlsBean.class)) {
                beans.put(bean.getName(), bean.getFieldValue(context));
                if (bean.getFixedSizeCollection())
                    fixedSizeCollectionBeanNames.add(bean.getName());
            }
            beans.put(DATA_BINDING, data);
            beans.put(DATA_CONTEXT_BINDING, context);
            beans.putAll(beansStore.get());
            List<SheetInfo> sheets = sheetsStore.get();
            if (sheets==null){
                sheets = new LinkedList<SheetInfo>();
                sheetsStore.set(sheets);
            }
            sheets.add(new SheetInfo(beans, fixedSizeCollectionBeanNames, templateSheetName, sheetName));
            if (!multiSheetReport)
                generateReport(context);
        }finally{
            bindingSupport.reset();
            beansStore.remove();
        }
    }

    private void generateReport(DataContext context) throws Exception
    {
        try{
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Generating report");
            List<SheetInfo> sheets = sheetsStore.get();

            if (sheets==null){
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    getLogger().debug("No data to generate excel report. Skipping");
                return ;
            }

            List<String> templateSheetNames = new ArrayList<String>(sheets.size());
            List<String> sheetNames = new ArrayList<String>(sheets.size());
            List<Map> sheetBeans = new ArrayList<Map>(sheets.size());
            XLSTransformer transformer = new XLSTransformer();
            for (SheetInfo info: sheets){
                templateSheetNames.add(info.templateSheetName);
                sheetNames.add(info.sheetName);
                sheetBeans.add(info.beans);
                for (String name: info.fixedSizeCollectionBeanNames)
                    transformer.markAsFixedSizeCollection(name);
            }

            List<CellStyleSelectorNode> styleSelectors = NodeUtils.getChildsOfType(
                    this, CellStyleSelectorNode.class);
            if (!styleSelectors.isEmpty()){
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    getLogger().debug("Found style selectors. Registiring row processor");
                transformer.registerRowProcessor(new CellStyleProcessor(styleSelectors, bindingSupport, this));
            }
            HSSFWorkbook wb = (HSSFWorkbook) transformer.transformXLS(
                    reportTemplate.getDataStream(), templateSheetNames, sheetNames, sheetBeans);

//            wb.getSheetAt(0).createFreezePane(5, 1);

            File tempFile = File.createTempFile("jxls_"+getId()+"_", ".xls");
            try{
                FileDataSource ds = new FileDataSource(tempFile);
                OutputStream os = ds.getOutputStream();
                try{
                    wb.write(os);
                }finally{
                    os.close();
                }
                InputStream is = ds.getInputStream();
                try{
                    Object res = ds;
                    String _reportFieldName = reportFieldName;
                    if (   _reportFieldName!=null
                        && sheetBeans.size()==1 
                        && (sheetBeans.get(0).get(DATA_BINDING) instanceof Record))
                    {
                        Record rec = (Record) sheetBeans.get(0).get(DATA_BINDING);
                        RecordSchemaField field = RavenUtils.getRecordSchemaField(
                                rec.getSchema(), _reportFieldName);
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
        }finally{
            sheetsStore.remove();
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

    public Boolean getMultiSheetReport() {
        return multiSheetReport;
    }

    public void setMultiSheetReport(Boolean multiSheetReport) {
        this.multiSheetReport = multiSheetReport;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
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

    public String getTemplateSheetName() {
        return templateSheetName;
    }

    public void setTemplateSheetName(String templateSheetName) {
        this.templateSheetName = templateSheetName;
    }

    private class SheetInfo {
        private final Map beans;
        private final Set<String> fixedSizeCollectionBeanNames;
        private final String templateSheetName;
        private final String sheetName;

        public SheetInfo(Map beans, Set<String> fixedSizeCollectionBeanNames
                , String templateSheetName, String sheetName) 
        {
            this.beans = beans;
            this.fixedSizeCollectionBeanNames = fixedSizeCollectionBeanNames.isEmpty()?
                    Collections.EMPTY_SET : fixedSizeCollectionBeanNames;
            this.templateSheetName = templateSheetName;
            this.sheetName = sheetName;
        }

    }
}
