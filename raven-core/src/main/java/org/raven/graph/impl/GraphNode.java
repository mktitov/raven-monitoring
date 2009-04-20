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

package org.raven.graph.impl;

import java.util.Arrays;
import org.raven.graph.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jrobin.core.Util;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;
import org.raven.ImageFormat;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.IfNode;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=
    {
        IfNode.class, CommentNode.class, HorizontalRuleNode.class, CalculatedDataDefNode.class,
        AreaNode.class, LineNode.class, StackNode.class, PrintNode.class, RecordsDataDef.class,
        SdbQueryResultDataDef.class
    })
public class GraphNode extends BaseNode implements Viewable
{
    public static final String SIGNATURE = "Raven-monitoring";
    public final static String STARTIME_ATTRIBUTE = "startTime";
    public final static String ENDTIME_ATTRIBUTE = "endTime";
    
    @Parameter
    private String title;

	@Parameter(defaultValue="false")
	private Boolean enableAntialiasing;

    @Parameter(defaultValue="end-1d")
    @NotNull
    private String startTime;

    @Parameter(defaultValue="now")
    @NotNull
    private String endTime;

    @Parameter @NotNull
    private Integer height;

    @Parameter @NotNull
    private Integer width;

    @Parameter(defaultValue="PNG")
    @NotNull
    private ImageFormat imageFormat;

    @Parameter
    private String verticalLabel;

    @Parameter
    private Double maxValue;

    @Parameter
    private Double minValue;

    @Parameter
    private String unit;

    @Parameter
    private Integer unitsExponent;

    @Parameter(defaultValue="true")
    @NotNull
    private Boolean autoRefresh;

    @Parameter(defaultValue="true")
    @NotNull
    private Boolean generateRefreshAttributes;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean detectTimeIntervalFromData;

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        if (getStatus()!=Status.STARTED || !generateRefreshAttributes)
            return null;

        String attrDesc =
                descriptorRegistry.getPropertyDescriptor(this.getClass(), STARTIME_ATTRIBUTE)
                .getDescription();
        int id=1;
        NodeAttributeImpl startTimeAttr = new NodeAttributeImpl(
                STARTIME_ATTRIBUTE, String.class, startTime, attrDesc);
        startTimeAttr.setId(id++);
        startTimeAttr.setOwner(this);
        startTimeAttr.setFireEvents(false);
        startTimeAttr.setValue(startTime);
        startTimeAttr.init();

        attrDesc =
                descriptorRegistry.getPropertyDescriptor(this.getClass(), ENDTIME_ATTRIBUTE)
                .getDescription();
        NodeAttributeImpl endTimeAttr = new NodeAttributeImpl(
                ENDTIME_ATTRIBUTE, String.class, endTime, attrDesc);
        endTimeAttr.setId(id++);
        endTimeAttr.setOwner(this);
        endTimeAttr.setFireEvents(false);
        endTimeAttr.setValue(endTime);
        endTimeAttr.init();

        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        attrs.put(startTimeAttr.getName(), startTimeAttr);
        attrs.put(endTimeAttr.getName(), endTimeAttr);

        return attrs;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
    {
        if (Status.STARTED!=getStatus())
            return null;

        String sTime = null;
        String eTime = null;
        if (refreshAttributes!=null && generateRefreshAttributes)
        {
            sTime = getAttributeValue(refreshAttributes, STARTIME_ATTRIBUTE);
            eTime = getAttributeValue(refreshAttributes, ENDTIME_ATTRIBUTE);
        }

        ViewableObject viewableObject = new GraphViewableObject(this, sTime, eTime);

        return Arrays.asList(viewableObject);
    }

    private String getAttributeValue(Map<String, NodeAttribute> attrs, String name)
    {
        NodeAttribute attr = attrs.get(name);
        return attr==null? null : attr.getValue();
    }

    public byte[] render(String startTime, String endTime)
    {
        try
        {
            RrdGraphDef def = createGraphDef(startTime, endTime);
            RrdGraph graph = new RrdGraph(def);
            
            return graph.getRrdGraphInfo().getBytes();
        }
        catch (Exception ex)
        {
            String message = "Error generating graph";
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(message+".", ex);
            throw new NodeError(String.format(message+" (%s).",getPath()), ex);
        }
    }
    private RrdGraphDef createGraphDef(String startTime, String endTime) throws Exception
    {
        if (getChildrens()==null)
            return null;
        RrdGraphDef gdef = new RrdGraphDef();
        gdef.setShowSignature(true);
		gdef.setSignature(SIGNATURE);
		gdef.setAntiAliasing(enableAntialiasing);
        gdef.setTitle(title);
        gdef.setWidth(width);
        gdef.setHeight(height);
        gdef.setImageFormat(imageFormat.asString());
        gdef.setFilename("-");
        gdef.setVerticalLabel(verticalLabel);
        if (minValue!=null)
            gdef.setMinValue(minValue);
        if (maxValue!=null)
            gdef.setMaxValue(maxValue);
        if (unit!=null)
            gdef.setUnit(unit);
        if (unitsExponent!=null)
            gdef.setUnitsExponent(unitsExponent);

        String start = startTime==null? this.startTime : startTime;
        String end = endTime==null? this.endTime : endTime;

        Long[] timeInterval = null;
        boolean _detectTimeIntervalFromData = detectTimeIntervalFromData;
        if (_detectTimeIntervalFromData)
            timeInterval = new Long[]{null, null};
        else
        {
            long[] timeIntervalArr = Util.getTimestamps(start, end);
            timeInterval = new Long[2];
            for (int i=0; i<timeIntervalArr.length; ++i)
                timeInterval[i] = timeIntervalArr[i];
            gdef.setStartTime(timeInterval[0]);
            gdef.setEndTime(timeInterval[1]);
        }

        Collection<Node> gElements = getEffectiveChildrens();
        long[] dataTimeInterval = new long[]{0,0};
        if (gElements!=null)
        {
            for (Node node: gElements)
            {
                if (node.getStatus()!=Status.STARTED)
                    continue;

                if (node instanceof DataDef)
                    addDataDef(
                        gdef, (DataDef)node, timeInterval[0], timeInterval[1], dataTimeInterval);

                if (node instanceof DataDefGroup)
                {
                    Collection<DataDef> dataDefs =
                            ((DataDefGroup)node).getDataDefs(timeInterval[0], timeInterval[1]);
                    if (dataDefs!=null && !dataDefs.isEmpty())
                        for (DataDef dataDef: dataDefs)
                            addDataDef(gdef, dataDef, timeInterval[0], timeInterval[1]
                                , dataTimeInterval);
                } else if (node instanceof CalculatedDataDef)
                {
                    CalculatedDataDef cdef = (CalculatedDataDef) node;
                    gdef.datasource(cdef.getName(), cdef.getExpression());
                }
                else if (node instanceof HorizontalRule)
                {
                    HorizontalRule hrule = (HorizontalRule) node;
                    gdef.hrule(
                            hrule.getRulePosition(), hrule.getColor().getColor(), hrule.getLegend()
                            , hrule.getWidth());
                }
                else if (node instanceof Line)
                {
                    Line line = (Line) node;
                    gdef.line(line.getDataDefName(), line.getColor().getColor(), line.getLegend()
                            , line.getWidth());
                }
                else if (node instanceof Stack)
                {
                    Stack stack = (Stack) node;
                    gdef.stack(
                            stack.getDataDefName(), stack.getColor().getColor(), stack.getLegend());
                }
                else if (node instanceof Area)
                {
                    Area area = (Area) node;
                    gdef.area(area.getDataDefName(), area.getColor().getColor(), area.getLegend());
                }
                else if (node instanceof Print)
                {
                    Print print = (Print) node;
                    gdef.gprint(
                            print.getDataDefName(), print.getConsolidationFunction().asString()
                            , print.getFormat());
                }
                else if (node instanceof Comment)
                {
                    gdef.comment(((Comment)node).getComment());
                }
            }
        }
        if (_detectTimeIntervalFromData)
        {
            gdef.setStartTime(dataTimeInterval[0]);
            gdef.setEndTime(dataTimeInterval[1]);
        }
        return gdef;
    }

    private void addDataDef(
            RrdGraphDef graphDef, DataDef dataDef, Long start, Long end, long[] dataTimeInterval)
        throws DataDefException
    {
        GraphData graphData = dataDef.getData(start, end);
        graphDef.datasource(dataDef.getName(), graphData.getPlottable());
        if (dataTimeInterval[0]==0 || dataTimeInterval[0]>graphData.getFirstTimestamp())
            dataTimeInterval[0] = graphData.getFirstTimestamp();
        if (dataTimeInterval[1]<graphData.getLastTimestamp())
            dataTimeInterval[1] = graphData.getLastTimestamp();
    }

    public Boolean getDetectTimeIntervalFromData()
    {
        return detectTimeIntervalFromData;
    }

    public void setDetectTimeIntervalFromData(Boolean detectTimeIntervalFromData)
    {
        this.detectTimeIntervalFromData = detectTimeIntervalFromData;
    }

    public Boolean getGenerateRefreshAttributes()
    {
        return generateRefreshAttributes;
    }

    public void setGenerateRefreshAttributes(Boolean generateRefreshAttributes)
    {
        this.generateRefreshAttributes = generateRefreshAttributes;
    }

    public Boolean getAutoRefresh()
    {
        return autoRefresh;
    }

    public void setAutoRefresh(Boolean autoRefresh)
    {
        this.autoRefresh = autoRefresh;
    }

    public Boolean getEnableAntialiasing()
    {
        return enableAntialiasing;
    }

    public void setEnableAntialiasing(Boolean enableAntialiasing)
    {
        this.enableAntialiasing = enableAntialiasing;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime(String endTime)
    {
        this.endTime = endTime;
    }

    public Integer getHeight()
    {
        return height;
    }

    public void setHeight(Integer height)
    {
        this.height = height;
    }

    public ImageFormat getImageFormat()
    {
        return imageFormat;
    }

    public void setImageFormat(ImageFormat imageFormat)
    {
        this.imageFormat = imageFormat;
    }

    public Double getMaxValue()
    {
        return maxValue;
    }

    public void setMaxValue(Double maxValue)
    {
        this.maxValue = maxValue;
    }

    public Double getMinValue()
    {
        return minValue;
    }

    public void setMinValue(Double minValue)
    {
        this.minValue = minValue;
    }

    public String getStartTime()
    {
        return startTime;
    }

    public void setStartTime(String startTime)
    {
        this.startTime = startTime;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public Integer getUnitsExponent()
    {
        return unitsExponent;
    }

    public void setUnitsExponent(Integer unitsExponent)
    {
        this.unitsExponent = unitsExponent;
    }

    public String getVerticalLabel()
    {
        return verticalLabel;
    }

    public void setVerticalLabel(String verticalLabel)
    {
        this.verticalLabel = verticalLabel;
    }

    public Integer getWidth()
    {
        return width;
    }

    public void setWidth(Integer width)
    {
        this.width = width;
    }
}
