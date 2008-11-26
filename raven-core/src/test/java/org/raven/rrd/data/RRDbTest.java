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

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.jrobin.core.FetchData;
import org.jrobin.core.FetchRequest;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.Sample;
import org.jrobin.core.Util;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
@Ignore
public class RRDbTest extends Assert
{

	@Test
	public void normalizeTest() throws ParseException
	{
		SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy");
		Date date = fmt.parse("17-11-2008");
		long time = Util.normalize(date.getTime()/1000, 86400);
		System.out.println((date.getTime()/1000)%86400);
		System.out.println(Util.getTimestamp(2008, 11, 17)%86400);
		System.out.println(date);
		System.out.println(new Date(time*1000));
		fail();
	}

//    @Test
    public void test() throws Exception
    {
        new File("target/test.rrd").delete();
        
        long time = Util.getTime();
        RrdDef def = new RrdDef("target/test.rrd", time, 300);
        def.addDatasource("input", "GAUGE", 600, 0, Double.MAX_VALUE);
        def.addDatasource("output", "GAUGE", 600, 0, Double.MAX_VALUE);
        def.addArchive("AVERAGE", 0.5, 1, 600);
        
        RrdDb rrd = new RrdDb(def);
//        RrdDb rrd = new RrdDb("target/test.rrd");
        try
        {
            Sample sample = rrd.createSample(time+300);

            // put datasource values in your sample
            sample.setValue("input", 100.); // or: sample.setValue(0, inputValue)
//            sample.setValue("output", 200.); // or: sample.setValue(1, outputValue);
            // update rrd file
            sample.update();
//            rrd.close();
//
//            rrd = new RrdDb("target/test.rrd");
            printArchiveData(time, rrd);

            sample = rrd.createSample(time+301);

            // put datasource values in your sample
            sample.setValue("input", 50.); // or: sample.setValue(0, inputValue)
            sample.setValue("output", 111.); // or: sample.setValue(1, outputValue);

            // update rrd file
            sample.update();
            printArchiveData(time, rrd);

        }
        finally
        {
            rrd.close();
        }
    }

    private void printArchiveData(long fromTime, RrdDb db) throws Exception
    {
//        TimeUnit.MICROSECONDS.sleep(500);
        System.out.println("----------Fetch--------");
        FetchRequest req = db.createFetchRequest("AVERAGE", fromTime, fromTime+600);
        FetchData fetchData = req.fetchData();
        for (String dsname: new String[]{"input", "output"})
        {
            System.out.println(">>> "+dsname);
            double[] values = fetchData.getValues(dsname);
            for (double value: values)
            {
                System.out.print(value+"\t");
            }
            System.out.println();
        }
    }
}
