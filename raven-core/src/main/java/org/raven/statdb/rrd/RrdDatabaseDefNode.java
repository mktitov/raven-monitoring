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

package org.raven.statdb.rrd;

import org.jrobin.core.ArcDef;
import org.jrobin.core.DsDef;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.jrobin.core.Util;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.rrd.DataSourceType;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class,
    childNodes={RrdDatasourceDefNode.class, RrdArchiveDefNode.class})
public class RrdDatabaseDefNode extends BaseNode
{
    @Parameter()
	@NotNull
    private Long step;

	@Parameter()
	@NotNull
	private String startTime;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();

        generateNode();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        generateNode();
    }

	public String getStartTime()
	{
		return startTime;
	}

	public void setStartTime(String startTime)
	{
		this.startTime = startTime;
	}

	public Long getStep()
	{
		return step;
	}

	public void setStep(Long step)
	{
		this.step = step;
	}

	public RrdDb createDatabase(String databasePath) throws Exception
	{
		String _startTime = startTime;
		long start = 0l;
		if (!"epoch".equals(_startTime))
			start = Util.getTimestamp(startTime);
        RrdDef def = new RrdDef(databasePath, start, step);

        boolean hasDataSources = false;
        boolean hasArchives = false;

        for (Node node : getChildrens())
        {
            if (node instanceof RrdDatasourceDefNode)
            {
                hasDataSources = true;
                RrdDatasourceDefNode ds = (RrdDatasourceDefNode) node;
                def.addDatasource(createDsDef(ds));
            }
            else if (node instanceof RrdArchiveDefNode)
            {
                hasArchives = true;
                RrdArchiveDefNode ar = (RrdArchiveDefNode) node;
                def.addArchive(createArcDef(ar));
            }
        }

        if (!hasDataSources || !hasArchives)
        {
            return null;
        }

        RrdDb db = new RrdDb(def);

		return db;
	}

    public static long[] getTimePeriod(String startTime, String endTime) throws Exception
    {
        long[] period = new long[]{-1, -1};
        if (startTime.endsWith("L"))
            period[0] = Long.parseLong(startTime.substring(0, startTime.length()-1));
        if (endTime.endsWith("L"))
            period[1] = Long.parseLong(endTime.substring(0, endTime.length()-1));

        if (period[0]==-1 && period[1]==-1)
            period = Util.getTimestamps(startTime, endTime);
        else
        {
            if (period[0]==-1)
                period[0] = Util.getTimestamp(startTime);
            if (period[1]==-1)
                period[1] = Util.getTimestamp(endTime);
        }

        return period;
    }

    private DsDef createDsDef(RrdDatasourceDefNode ds) throws Exception
    {
        if (ds.getHeartbeat()==null)
			ds.setHeartbeat(step*2);
		
        DsDef def = new DsDef(
                ds.getName(), ds.getDataSourceType().asString(), ds.getHeartbeat()
                , ds.getMinValue(), ds.getMaxValue());
        return def;
    }

    private ArcDef createArcDef(RrdArchiveDefNode archive) throws RrdException
    {
        String conFun = converter.convert(String.class, archive.getConsolidationFunction(), null);
        ArcDef def = new ArcDef(conFun, archive.getXff(), archive.getSteps(), archive.getRows());
        return def;
    }

    private void generateNode()
    {
        RrdDatasourceDefNode datasource =
                (RrdDatasourceDefNode) getChildren(RrdStatisticsDatabaseNode.DATASOURCE_NAME);
        if (datasource==null)
        {
            datasource = new RrdDatasourceDefNode();
            datasource.setName(RrdStatisticsDatabaseNode.DATASOURCE_NAME);
            addAndSaveChildren(datasource);
            datasource.setDataSourceType(DataSourceType.GAUGE);
            datasource.start();
        }
    }

}
