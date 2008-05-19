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

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.jrobin.core.Util;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;
import org.jrobin.graph.RrdGraphInfo;
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
public class RRGraphNode extends BaseNode
{
    @Parameter
    @Description("The time when the graph should begin")
    private String startTime = "end-1d";
    @Parameter
    @Description("The time when the graph should end")
    private String endTime = "now";
    @Parameter
    @NotNull
    @Description("The height of the drawing area within the graph")
    private Integer height;
    @Parameter
    @NotNull
    @Description("The width of the drawing area within the graph")
    private Integer width;
    
    
    public RRGraphNode()
    {
        super(new Class[]{RRDef.class/*, RRCdef.class, RRVdef.class*/}, true, false);
    }
    
    public InputStream render(String startTime, String endTime)
    {
        try
        {
            RrdGraphDef def = createGraphDef(startTime, endTime);
            RrdGraph graph = new RrdGraph(def);
            RrdGraphInfo info = graph.getRrdGraphInfo();
            
            BufferedImage image = 
                    new BufferedImage(
                        info.getWidth(), info.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            
            graph.render(image.createGraphics());
            ImageIO.write
        } 
        catch (Exception ex)
        {
            throw new NodeError(
                    String.format("Error generating graph (%s)", getPath()), ex);
        }
        return null;
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
    
    private RrdGraphDef createGraphDef(String startTime, String endTime) throws Exception
    {
        if (getChildrens()!=null)
        {
            RrdGraphDef gdef = new RrdGraphDef();
            gdef.setWidth(width);
            gdef.setHeight(height);
            
            if (startTime==null && this.startTime==null)
                throw new NodeError("startTime attribute must be seted");
            if (endTime==null && this.endTime==null)
                throw new NodeError("endTime attribute must be seted");
            
            String start = startTime==null? this.startTime : startTime;
            String end = endTime==null? this.endTime : endTime;
            
            long[] timeInterval = Util.getTimestamps(start, end);
            
            gdef.setStartTime(timeInterval[0]);
            gdef.setEndTime(timeInterval[1]);
            
            for (Node node: getChildrens())
            {
                if (node.getStatus()!=Status.STARTED)
                    continue;
                
                if (node instanceof RRDef)
                {
                    RRDef def = (RRDef) node;
                    RRDNode rrd = (RRDNode) def.getDataSource().getParent();
                    gdef.datasource(
                            def.getName(), rrd.getDatabaseFileName()
                            , def.getDataSource().getName()
                            , def.getConsolidationFunction().asString());
                } 
                else if (node instanceof RRLine)
                {
                    RRLine line = (RRLine) node;
                    if (line.getDataDefinition().getStatus()==Status.STARTED)
                        gdef.line(
                                line.getDataDefinition().getName(), line.getColor().getColor()
                                , line.getLegend(), line.getWidth());
                }
            }
            return null;
        } 
        else
        {
            return null;
        }
    }
    
}
