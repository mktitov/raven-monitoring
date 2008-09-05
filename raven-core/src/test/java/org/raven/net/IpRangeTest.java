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

package org.raven.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class IpRangeTest extends Assert
{
    @Test(expected=InvalidIpException.class)
    public void invalidIpExceptionTest1() throws Exception
    {
        IpRange range = new IpRange("1.1.1", "1.1.1.1");
    }
    
    @Test(expected=InvalidIpException.class)
    public void invalidIpExceptionTest2() throws Exception
    {
        IpRange range = new IpRange("1.1.1.256", "1.1.1.1");
    }

    @Test(expected=InvalidIpRangeException.class)
    public void invalidRangeException() throws Exception
    {
        IpRange range = new IpRange("1.1.1.2", "1.1.1.1");
    }

    @Test
    public void validRange() throws Exception
    {
        IpRange range = new IpRange("1.1.1.1", "1.1.1.2");
        range = new IpRange("1.1.1.1", "1.1.2.1");
        range = new IpRange("1.1.1.1", "2.1.1.1");
    }

    @Test
    public void iteratorTest() throws Exception
    {
        IpRange range = new IpRange("1.1.1.0", "1.1.2.2");
        List<String> list = new ArrayList<String>(259);
        for (int i=0; i<256; ++i)
            list.add("1.1.1."+i);
        list.add("1.1.2.0");
        list.add("1.1.2.1");
        list.add("1.1.2.2");

        int i=0;
        for (Iterator<String> it=range.getIterator(); it.hasNext();)
            assertEquals(list.get(i++), it.next());
    }
}
