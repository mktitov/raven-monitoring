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
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.jett.event.SheetListener;
import net.sf.jett.transform.ExcelTransformer;
import org.apache.poi.ss.usermodel.Workbook;
import org.h2.util.IOUtils;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.cache.TemporaryFileManager;
import org.raven.cache.TemporaryFileManagerValueHandlerFactory;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.IfNode;
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
        JxlsAttributeValueBeanNode.class, JxlsDataSourceBeanNode.class, SheetListenerNode.class,
        IfNode.class})
public class JettReportNode extends AbstractSafeDataPipe implements Viewable
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
    
    @Parameter
    private String styles;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean evaluateFormulas;
    
    @Parameter
    private Boolean forceRecalculationOnOpening;
    
    @NotNull @Parameter(valueHandlerType = TemporaryFileManagerValueHandlerFactory.TYPE)
    private TemporaryFileManager temporaryFileManager;

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
        if (data==null) {
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
            for (JxlsBean bean: NodeUtils.getEffectiveChildsOfType(this, JxlsBean.class)) {
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
            else 
                DataSourceHelper.executeContextCallbacks(this, context, data);
        } finally {
            bindingSupport.reset();
            beansStore.remove();
        }
    }

    private void generateReport(DataContext context) throws Exception {
        try { 
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
            List<Map<String, Object>> sheetBeans = new ArrayList<Map<String, Object>>(sheets.size());
            
            //create and init transformer
            ExcelTransformer transformer = new ExcelTransformer();
            final String _styles = styles;
            if (_styles!=null)
                transformer.addCssText(_styles);
            transformer.setEvaluateFormulas(evaluateFormulas);
            final Boolean _forceRecalculationOnOpening = forceRecalculationOnOpening;
            if (_forceRecalculationOnOpening!=null)
                transformer.setForceRecalculationOnOpening(_forceRecalculationOnOpening);
            
            //add sheet listeners
            for (SheetListener sheetListener: NodeUtils.getEffectiveChildsOfType(this, SheetListener.class))
                transformer.addSheetListener(sheetListener);
            
            for (SheetInfo info: sheets){
                templateSheetNames.add(info.templateSheetName);
                sheetNames.add(info.sheetName);
                sheetBeans.add(info.beans);
                for (String name: info.fixedSizeCollectionBeanNames)
                    transformer.addFixedSizeCollectionName(name);
            }            
            Workbook wb = transformer.transform(reportTemplate.getDataStream(), templateSheetNames, sheetNames, sheetBeans);
            final String fileKey = RavenUtils.generateUniqKey("jett_report_");
            TemporaryFileManager tempFileManager = temporaryFileManager;
            File outFile = tempFileManager.createFile(this, fileKey, "application/vnd.ms-excel");
            FileOutputStream outStream = new FileOutputStream(outFile);
            try {
                wb.write(outStream);
                sendDataToConsumers(tempFileManager.getDataSource(fileKey), context);
            } finally {
                IOUtils.closeSilently(outStream);
            }
        } finally {
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

    public TemporaryFileManager getTemporaryFileManager() {
        return temporaryFileManager;
    }

    public void setTemporaryFileManager(TemporaryFileManager temporaryFileManager) {
        this.temporaryFileManager = temporaryFileManager;
    }

    public String getStyles() {
        return styles;
    }

    public void setStyles(String styles) {
        this.styles = styles;
    }

    public Boolean getEvaluateFormulas() {
        return evaluateFormulas;
    }

    public void setEvaluateFormulas(Boolean evaluateFormulas) {
        this.evaluateFormulas = evaluateFormulas;
    }

    public Boolean getForceRecalculationOnOpening() {
        return forceRecalculationOnOpening;
    }

    public void setForceRecalculationOnOpening(Boolean forceRecalculationOnOpening) {
        this.forceRecalculationOnOpening = forceRecalculationOnOpening;
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
