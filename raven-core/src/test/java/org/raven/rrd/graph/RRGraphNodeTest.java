/*
 *  Copyright 2008 Milhail Titov.
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

import java.awt.Color;
import org.junit.Test;
import org.raven.ServiceTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class RRGraphNodeTest extends ServiceTestCase
{
    @Test
    public void getColor()
    {
//        Color color = Color.RED.;
        assertNotNull(Color.getColor("red"));
    }
}
