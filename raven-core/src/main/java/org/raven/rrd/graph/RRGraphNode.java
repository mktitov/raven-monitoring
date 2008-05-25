/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.rrd.graph;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.jrobin.core.Util;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.rrd.data.RRDNode;
import org.raven.tree.Node;
import org.raven.tree.NodeError;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
@Description("Plots the graph using RRDataSource nodes as data sources")
public class RRGraphNode extends BaseNode
{
    @Parameter @Description("Title")
    private String title;
    
    @Parameter @Description("The time when the graph should begin")
    private String startTime = "end-1d";
    
    @Parameter @Description("The time when the graph should end")
    private String endTime = "now";
    
    @Parameter @NotNull
    @Description("The height of the drawing area within the graph")
    private Integer height;
    
    @Parameter @NotNull @Description("The width of the drawing area within the graph")
    private Integer width;
    
    @Parameter @NotNull @Description("The image format (PNG, GIF, JPEG)")
    private ImageFormat imageFormat = ImageFormat.PNG;
    
    @Parameter @Description("Sets vertical label on the left side of the graph")
    private String verticalLabel;
    
    @Parameter @Description("Sets the upper limit of a graph")
    private Double maxValue;
    
    @Parameter @Description("Sets the lower limit of a grap.")
    private Double minValue;
    
    @Parameter @Description("Sets unit to be displayed on y axis")
    private String unit;
    @Parameter @Description("Sets the 10**unitsExponent scaling of the y-axis values")
    private Integer unitsExponent;
    
    public RRGraphNode()
    {
        super(
            new Class[]{
                RRDef.class, RRLine.class, RRCDef.class, RRComment.class, RRGPrint.class,
                RRHRule.class, RRStack.class}
            , true, false);
    }
    
    public InputStream render(String startTime, String endTime)
    {
        try
        {
            if (getStatus()!=Status.STARTED)
                throw new NodeError("Node not started");
            
            GraphDef def = createGraphDef(startTime, endTime);
            
            for (int i=0; i<def.rrdNodes.size(); ++i)
            {
                RRDNode rrd = def.rrdNodes.get(i);
                if (!rrd.getReadLock().tryLock())
                {
                    for (int j=0; j<i; ++j)
                        def.rrdNodes.get(j).getReadLock().unlock();
                    throw new NodeError(
                            String.format("Error lock rrd (%s) node for read.", rrd.getPath()));
                }
            }
            
            try
            {
                RrdGraph graph = new RrdGraph(def.graphDef);
                return new ByteArrayInputStream(graph.getRrdGraphInfo().getBytes());
            }
            finally
            {
                for (RRDNode rrd: def.rrdNodes)
                    rrd.getReadLock().unlock();
            }
        } 
        catch (Exception ex)
        {
            throw new NodeError(
                    String.format("Error generating graph (%s)", getPath()), ex);
        }
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

    public String getStartTime()
    {
        return startTime;
    }

    public void setStartTime(String startTime)
    {
        this.startTime = startTime;
    }

    public Integer getWidth()
    {
        return width;
    }

    public void setWidth(Integer width)
    {
        this.width = width;
    }

    public ImageFormat getImageFormat()
    {
        return imageFormat;
    }

    public void setImageFormat(ImageFormat imageFormat)
    {
        this.imageFormat = imageFormat;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
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

    public String getVerticalLabel()
    {
        return verticalLabel;
    }

    public void setVerticalLabel(String verticalLabel)
    {
        this.verticalLabel = verticalLabel;
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
    
    private GraphDef createGraphDef(String startTime, String endTime) throws Exception
    {
        if (getChildrens()==null)
            return null;
        GraphDef graphDef = new GraphDef();
        RrdGraphDef gdef = new RrdGraphDef();
        graphDef.graphDef = gdef;
        gdef.setShowSignature(false);
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

        String strt = this.startTime;
        if (startTime==null && strt==null)
            throw new NodeError("startTime attribute must be seted");
        if (endTime==null && this.endTime==null)
            throw new NodeError("endTime attribute must be seted");

        String start = startTime==null? this.startTime : startTime;
        String end = endTime==null? this.endTime : endTime;

        long[] timeInterval = Util.getTimestamps(start, end);

        gdef.setStartTime(timeInterval[0]);
        gdef.setEndTime(timeInterval[1]);

        for (Node node: getSortedChildrens())
        {
            if (node.getStatus()!=Status.STARTED)
                continue;

            if (node instanceof RRDef)
            {
                RRDef def = (RRDef) node;
                RRDNode rrd = (RRDNode) def.getDataSource().getParent();
                graphDef.rrdNodes.add(rrd);
                gdef.datasource(
                        def.getName(), rrd.getDatabaseFileName()
                        , def.getDataSource().getName()
                        , def.getConsolidationFunction().asString());
            } 
            else if (node instanceof RRCDef)
            {
                RRCDef cdef = (RRCDef) node;
                gdef.datasource(cdef.getName(), cdef.getExpression());
            }
            else if (node instanceof RRLine)
            {
                RRLine line = (RRLine) node;
                if (line.getDataDefinition().getStatus()==Status.STARTED)
                    gdef.line(
                            line.getDataDefinition().getName(), line.getColor().getColor()
                            , line.getLegend(), line.getWidth());
            }
            else if (node instanceof RRStack)
            {
                RRStack stack = (RRStack) node;
                if (stack.getDataDefinition().getStatus()==Status.STARTED)
                    gdef.stack(
                            stack.getDataDefinition().getName(), stack.getColor().getColor()
                            , stack.getLegend());
            }
            else if (node instanceof RRArea)
            {
                RRArea area = (RRArea) node;
                if (area.getDataDefinition().getStatus()==Status.STARTED)
                    gdef.area(
                            area.getDataDefinition().getName(), area.getColor().getColor()
                            , area.getLegend());
            }
            else if (node instanceof RRComment)
            {
                RRComment comment = (RRComment) node;
                gdef.comment(comment.getComment());
            }
            else if (node instanceof RRGPrint)
            {
                RRGPrint gprint = (RRGPrint) node;
                if (gprint.getDataDefinition().getStatus()==Status.STARTED)
                    gdef.gprint(
                            gprint.getDataDefinition().getName()
                            , gprint.getConsolidationFunction().asString()
                            , gprint.getFormat());
            }
            else if (node instanceof RRHRule)
            {
                RRHRule hrule = (RRHRule) node;
                gdef.hrule(
                        hrule.getValue(), hrule.getColor().getColor(), hrule.getLegend()
                        , hrule.getWidth());
            }
        }
        return graphDef;
    }
    
    private class GraphDef 
    {
        public RrdGraphDef graphDef;
        public List<RRDNode> rrdNodes = new ArrayList<RRDNode>(5);
    }
    
}
