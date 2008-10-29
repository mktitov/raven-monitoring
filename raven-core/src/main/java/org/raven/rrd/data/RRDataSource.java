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

package org.raven.rrd.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.ArchiveException;
import org.raven.ds.DataArchive;
import org.raven.ds.impl.DataPipeImpl;
import org.raven.rrd.DataSourceType;
import org.raven.table.DataArchiveTable;
import org.raven.table.Table;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.Description;
import org.weda.internal.Messages;
import org.weda.internal.annotations.Service;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RRDNode.class)
//@Description("Round robin database data source node")
public class RRDataSource extends DataPipeImpl implements DataArchive, Viewable
{

    public final static String DATASOURCETYPE_ATTRIBUTE = "dataSourceType";

    public final static String FROMDATE_ATTRIBUTE = "fromDate";
    public final static String TODATE_ATTRIBUTE = "toDate";

    @Service
    private MessagesRegistry messages;

    @Parameter(defaultValue="GAUGE")
    private DataSourceType dataSourceType;
    
    @Parameter
//    @Description("The data source heartbeat")
    private Long heartbeat;
    
    @Parameter(defaultValue="NaN")
//    @Description("Minimal acceptable value")
    private Double minValue;

    @Parameter(defaultValue="NaN")
//    @Description("Maximal acceptable value")
    private Double maxValue;

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue) 
    {
        super.nodeAttributeValueChanged(node, attribute, oldValue, newValue);
        
        if (   node==this 
            && newValue==null 
            && getStatus()==Status.STARTED 
            && DATASOURCE_ATTRIBUTE.equals(attribute.getName()))
        {
            setStatus(Status.INITIALIZED);
        }
    }

    public DataSourceType getDataSourceType()
    {
        return dataSourceType;
    }

	public void setDataSourceType(DataSourceType dataSourceType)
	{
		this.dataSourceType = dataSourceType;
	}

    public Long getHeartbeat()
    {
        return heartbeat;
    }

	public void setHeartbeat(Long heartbeat)
	{
		this.heartbeat = heartbeat;
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

    public DataArchiveTable getArchivedData(String fromDate, String toDate) throws ArchiveException
    {
        return ((RRDNode)getEffectiveParent()).getArchivedData(this, fromDate, toDate);
    }

    @Override
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();

        Messages dataArchiveMessages = messages.getMessages(DataArchive.class);

        NodeAttributeImpl attr =new NodeAttributeImpl(
                FROMDATE_ATTRIBUTE, String.class
                , "end-1d", dataArchiveMessages.get("fromDateDescription"));
        attr.setId(-1);
        attr.setOwner(this);
        attr.init();

        attrs.put(attr.getName(), attr);

        attr = new NodeAttributeImpl(
                TODATE_ATTRIBUTE, String.class
                , "now", dataArchiveMessages.get("toDateDescription"));
        attr.setId(-2);
        attr.setOwner(this);
        attr.init();

        attrs.put(attr.getName(), attr);

        return attrs;
    }

    @Override
    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        List<ViewableObject> viewableObjects = super.getViewableObjects(refreshAttributes);
        
        String fromDate = refreshAttributes.get(FROMDATE_ATTRIBUTE).getRealValue();
        String toDate = refreshAttributes.get(TODATE_ATTRIBUTE).getRealValue();

        Table table = getArchivedData(fromDate, toDate);
        ViewableObject viewableObject =
                new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);

        List<ViewableObject> result = new ArrayList<ViewableObject>(2);
        if (viewableObjects!=null)
            result.addAll(viewableObjects);
        result.add(viewableObject);

        return result;
    }
}
