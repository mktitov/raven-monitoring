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

package org.raven;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class StrSubstTest extends Assert
{
    @Test
    public void test()
    {
        StrSubstitutor subst = new StrSubstitutor(new VarStrLookup());
        String result = subst.replace("${num}, ${num}");
        assertEquals("1, 2", result);
    }

    public class VarStrLookup extends StrLookup
    {
        private String[] vals = new String[]{"1", "2"};
        private int i=0;
        
        @Override
        public String lookup(String key)
        {
            return vals[i++];
        }
    }
}
