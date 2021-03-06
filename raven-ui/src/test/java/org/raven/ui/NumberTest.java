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

package org.raven.ui;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class NumberTest extends Assert
{
    @Test
    public void test()
    {
        int n=1002;
        int grp = n/1000;
        assertEquals(1, grp);
        int col = (int) ((n / 1000. - grp) * 1000);
        assertEquals(2, col);
    }
    
    @Test public void mapToStringTest() {
        Map map = new HashMap();
        map.put("test", 1);
        map.put("test2", "2");
        System.out.println("map: "+map);
    }
}
