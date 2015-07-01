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

package org.raven.ds.impl;

import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractRecordFieldExtensionTest extends RavenCoreTestCase {
    AbstractRecordFieldExtension ext;
    
    @Before
    public void prepare() {
        ext = new AbstractRecordFieldExtension();
        ext.setName("ext");
        testsNode.addAndSaveChildren(ext);
    }
    
    @Test
    public void prepareValueTest() {
        assertEquals("1", ext.prepareValue("1", null));
        ValuePrepareRecordFieldExtension valuePrepare = new ValuePrepareRecordFieldExtension();
        valuePrepare.setName("prepare");
        ext.addAndSaveChildren(valuePrepare);
        valuePrepare.setConvertToType(Integer.class);
        valuePrepare.start();
        assertEquals(Status.STARTED, valuePrepare.getStatus());
        
        assertEquals(1, ext.prepareValue("1", null));
    }
    
    @Test
    public void getCodecTest() {
        assertNull(ext.getCodec());
        
        RecordSchemaFieldCodecNode codec = new RecordSchemaFieldCodecNode();
        codec.setName("codec");
        ext.addAndSaveChildren(codec);
        assertTrue(codec.start());
        assertSame(codec, ext.getCodec());
    }
}