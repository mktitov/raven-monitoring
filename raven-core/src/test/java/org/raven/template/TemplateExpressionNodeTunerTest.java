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

package org.raven.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.script.Bindings;
import org.easymock.IArgumentMatcher;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeTuner;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class TemplateExpressionNodeTunerTest extends RavenCoreTestCase
{
    private Node sourceNode;
    private Node sourceClone;
    private Node templateNode;
    private Node templateNode2;
    private NodeAttribute attr;
    private NodeAttribute attr2;
    private NodeAttribute attrClone;
    
    @Test
    public void templateExpression_in_attribute()
    {
        createMocks();
        
        NodeTuner tuner = new TemplateExpressionNodeTuner();
        tuner.tuneNode(sourceNode, sourceClone);
        
        verify(sourceNode, sourceClone, templateNode, attr, attr2, attrClone);
    }
    
    @Test
    public void unequals_workTemplate_and_sourceNodeTemplate()
    {
        createMocks2();
        
        NodeTuner tuner = new TemplateExpressionNodeTuner();
        tuner.tuneNode(sourceNode, sourceClone);
        
        verify(sourceNode, sourceClone, templateNode, templateNode2);
    }
    
    @Test
    public void nodeNameWithTemplateExpression() throws CloneNotSupportedException
    {
        createMocks3();
        
        NodeTuner tuner = new TemplateExpressionNodeTuner();
        assertSame(tuner.cloneNode(sourceNode), sourceClone);
        
        verify(sourceNode, sourceClone, templateNode);
    }
    
    @Test 
    public void nodeNameWithoutTemplateExpression()
    {
        createMocks4();
        
        NodeTuner tuner = new TemplateExpressionNodeTuner();
        assertNull(tuner.cloneNode(sourceNode));
        verify(sourceNode);
    }
    
    private void createMocks()
    {
        sourceNode = createMock("sourceNode", Node.class);
        sourceClone = createMock("sourceClone", Node.class);
        templateNode = createMock("templateNode", Node.class);
        attr = createMock("exprAttr", NodeAttribute.class);
        attr2 = createMock("notExprAttr", NodeAttribute.class);
        attrClone = createMock("exprAttrClone", NodeAttribute.class);
        
        sourceNode.formExpressionBindings(formExpressionBindings(templateNode));
        expect(sourceNode.getTemplate()).andReturn(templateNode);
        List<NodeAttribute> attrs = Arrays.asList(attr, attr2);
        expect(sourceNode.getNodeAttributes()).andReturn(attrs);
        expect(attr.isTemplateExpression()).andReturn(true);
        expect(attr.getRawValue()).andReturn("1+1");
        expect(attr.getName()).andReturn("exprAttr");
        
        expect(sourceClone.getNodeAttribute("exprAttr")).andReturn(attrClone);
        attrClone.setTemplateExpression(false);
        attrClone.setRawValue("2");
        
        expect(attr2.isTemplateExpression()).andReturn(false);
     
        replay(sourceNode, sourceClone, templateNode, attr, attr2, attrClone);
    }

    private static Bindings formExpressionBindings(final Node templateNode) 
    {
        reportMatcher(new IArgumentMatcher() 
        {
            public boolean matches(Object argument) 
            {
                Bindings bindings = (Bindings) argument;
                bindings.put(TemplateNode.TEMPLATE_EXPRESSION_BINDING, templateNode);
                return true;
            }

            public void appendTo(StringBuffer buffer) {
                ;
            }
        });
        
        return null;
    }
    
    private void createMocks2()
    {
        sourceNode = createMock("sourceNode", Node.class);
        sourceClone = createMock("sourceClone", Node.class);
        templateNode = createMock("templateNode", Node.class);
        templateNode2 = createMock("templateNode2", Node.class);
        
        sourceNode.formExpressionBindings(formExpressionBindings(templateNode));
        expect(sourceNode.getTemplate()).andReturn(templateNode2);
     
        replay(sourceNode, sourceClone, templateNode, templateNode2);
    }

    private void createMocks3() throws CloneNotSupportedException
    {
        sourceNode = createMock("sourceNode", Node.class);
        sourceClone = createMock("sourceClone", Node.class);
        templateNode = createMock("templateNode", Node.class);
        
        sourceNode.formExpressionBindings(formExpressionBindings(templateNode));
        expect(sourceNode.getTemplate()).andReturn(templateNode);
        expect(sourceNode.getName()).andReturn(
                TemplateExpressionNodeTuner.TEMPLATE_EXPRESSION_PREFIX+"'1'+'1'").times(2);
        expect(sourceNode.clone()).andReturn(sourceClone);
        sourceClone.setName("11");
        
        replay(sourceNode, sourceClone, templateNode);
    }
    
    private void createMocks4() 
    {
        sourceNode = createMock("sourceNode", Node.class);
        
        expect(sourceNode.getName()).andReturn("name without expression");
        
        replay(sourceNode);
    }
    
}
