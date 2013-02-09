/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.auth.impl;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class AccessControlNodeTest extends RavenCoreTestCase 
{
    private AccessControlNode controlNode;
    
    @Before
    public void prepare() {
        controlNode = new AccessControlNode();
        controlNode.setName("control");
        tree.getRootNode().addAndSaveChildren(controlNode);
        controlNode.setNode(controlNode);
    }
    
    @Test
    public void getAccessControlsOnNotStartedTest() {
        assertEquals(Collections.EMPTY_LIST, controlNode.getAccessControls());
    }
    
    @Test
    public void nodeAccessTest() {
        controlNode.setModifier(NodePathModifier.NODE_ONLY);
        controlNode.setRight(AccessRight.READ);
        assertTrue(controlNode.start());
        List<AccessControl> controls = controlNode.getAccessControls();
        assertEquals(1, controls.size());
        AccessControl control = controls.get(0);
        assertEquals(AccessControl.READ, control.getRight());
        assertEquals(controlNode.getPath(), control.getNodePath());
        checkRegexp(control, controlNode.getPath(), true);
        checkRegexp(control, controlNode.getPath()+"/child", false);
    }
    
    @Test
    public void childAccessTest() {
        controlNode.setModifier(NodePathModifier.CHILDREN_ONLY);
        controlNode.setRight(AccessRight.READ);
        assertTrue(controlNode.start());
        List<AccessControl> controls = controlNode.getAccessControls();
        assertEquals(1, controls.size());
        AccessControl control = controls.get(0);
        assertEquals(AccessControl.READ, control.getRight());
        assertEquals(controlNode.getPath(), control.getNodePath());
        checkRegexp(control, controlNode.getPath(), false);
        checkRegexp(control, controlNode.getPath()+"/child", true);
    }
    
    @Test
    public void nodeAndChildAccessTest() {
        controlNode.setModifier(NodePathModifier.NODE_and_CHILDREN);
        controlNode.setRight(AccessRight.READ);
        assertTrue(controlNode.start());
        List<AccessControl> controls = controlNode.getAccessControls();
        assertEquals(2, controls.size());
        
        AccessControl control = controls.get(0);
        assertEquals(AccessControl.READ, control.getRight());
        assertEquals(controlNode.getPath(), control.getNodePath());
        checkRegexp(control, controlNode.getPath(), true);
        checkRegexp(control, controlNode.getPath()+"/child", false);
        
        control = controls.get(1);
        assertEquals(AccessControl.READ, control.getRight());
        assertEquals(controlNode.getPath(), control.getNodePath());
        checkRegexp(control, controlNode.getPath(), false);
        checkRegexp(control, controlNode.getPath()+"/child", true);
    }
    
    private void checkRegexp(AccessControl control, String path, boolean expectedResult) {
        assertEquals(expectedResult, Pattern.matches(control.getRegExp(), path));
    }
    
}
