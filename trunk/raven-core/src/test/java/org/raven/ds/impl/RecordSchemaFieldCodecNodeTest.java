/*
 * Copyright 2014 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raven.ds.impl;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
/**
 *
 * @author Mikhail Titov
 */
public class RecordSchemaFieldCodecNodeTest extends RavenCoreTestCase {
    private RecordSchemaFieldCodecNode codec;
    
    @Before
    public void prepare() {
        codec = new RecordSchemaFieldCodecNode();
        codec.setName("codec");
        testsNode.addAndSaveChildren(codec);        
    }
    
    @Test
    public void encodeDecodeWithDisabledFlags() {
        assertTrue(codec.start());
        assertEquals(1, codec.encode(1, null));
        assertEquals(2, codec.decode(2, null));
    }
    
    @Test
    public void encodeWithoutBindingsTest() {
        codec.setEncodeExpression("value*10");
        codec.setUseEncodeExpression(Boolean.TRUE);
        assertTrue(codec.start());
        assertEquals(10, codec.encode(1, null));
    }
    
    @Test
    public void encodeWithBindingsTest() {
        codec.setEncodeExpression("value*factor");
        codec.setUseEncodeExpression(Boolean.TRUE);
        assertTrue(codec.start());
        Bindings bindings = new SimpleBindings();
        bindings.put("factor", 10);
        assertEquals(10, codec.encode(1, bindings));
    }
    
    @Test
    public void decodeWithoutBindingsTest() {
        codec.setDecodeExpression("value*10");
        codec.setUseDecodeExpression(Boolean.TRUE);
        assertTrue(codec.start());
        assertEquals(10, codec.decode(1, null));
    }
    
    @Test
    public void decodeWithBindingsTest() {
        codec.setDecodeExpression("value*factor");
        codec.setUseDecodeExpression(Boolean.TRUE);
        assertTrue(codec.start());
        Bindings bindings = new SimpleBindings();
        bindings.put("factor", 10);
        assertEquals(10, codec.decode(1, bindings));
    }
}
    
