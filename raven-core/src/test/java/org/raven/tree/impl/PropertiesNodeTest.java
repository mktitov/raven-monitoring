/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.tree.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class PropertiesNodeTest extends RavenCoreTestCase {
    private PropertiesNode propsNode;
    
    @Before
    public void prepare() {
        propsNode = new PropertiesNode();
        propsNode.setName("properties");
        testsNode.addAndSaveChildren(propsNode);        
    }
    
    @Test
    public void noCacheTest() throws Exception {
        propsNode.setCacheEnabled(false);
        assertNull(propsNode.getProperties());
        Properties props = createAndSave("p1","v1");
        
        assertTrue(propsNode.start());
        Properties res = propsNode.getProperties();
        assertEquals(props, res);
        Properties res2 = propsNode.getProperties();
        assertNotSame(res, res2);
        assertEquals(props, res2);
    }
    
    @Test
    public void cacheTest() throws Exception {
        propsNode.setCacheEnabled(Boolean.TRUE);
        Properties props = createAndSave("p1","v1");
        
        assertTrue(propsNode.start());
        Properties res = propsNode.getProperties();
        assertEquals(props, res);
        Properties res2 = propsNode.getProperties();
        assertSame(res, res2);
        
        createAndSave("p1","v1");
        Properties res3 = propsNode.getProperties();
        assertSame(res, res3);
        
        props = createAndSave("p1","v2");
        Properties res4 = propsNode.getProperties();
        assertNotSame(res, res4);
        assertEquals(props, res4);        
    }
    
    public Properties createAndSave(String... keyAndVals) throws Exception {
        Properties props = new Properties();
        for (int i=0; i<keyAndVals.length; i=+2)
            props.setProperty(keyAndVals[i], keyAndVals[i+1]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        propsNode.getPropertiesFile().setDataStream(new ByteArrayInputStream(out.toByteArray()));
        return props;
    }
}
