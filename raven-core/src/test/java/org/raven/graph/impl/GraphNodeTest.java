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

package org.raven.graph.impl;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.raven.ImageFormat;
import org.raven.RavenCoreTestCase;
import org.raven.graph.GraphColor;

/**
 *
 * @author Mikhail Titov
 */
public class GraphNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        GraphNode graph = new GraphNode();
        graph.setName("graph");
        tree.getRootNode().addAndSaveChildren(graph);
        graph.setTitle("Graph title");
        graph.setWidth(800);
        graph.setHeight(600);
        graph.setImageFormat(ImageFormat.PNG);
        graph.setMaxValue(15.);
        assertTrue(graph.start());

        CommentNode comment = new CommentNode();
        comment.setName("comment");
        graph.addAndSaveChildren(comment);
        comment.setComment("Graph comment");
        assertTrue(comment.start());

        HorizontalRuleNode hrule = new HorizontalRuleNode();
        hrule.setName("hrule");
        graph.addAndSaveChildren(hrule);
        hrule.setRulePosition(10.);
        hrule.setLegend("hrule legend");
        hrule.setColor(GraphColor.BLUE);
        hrule.setWidth(2.f);
        assertTrue(hrule.start());

        TestDataDef def = new TestDataDef();
        def.setName("def");
        graph.addAndSaveChildren(def);
        assertTrue(def.start());

        AreaNode area = new AreaNode();
        area.setName("area");
        graph.addAndSaveChildren(area);
        area.setDataDef(def);
        area.setLegend("area legend");
        area.setColor(GraphColor.GREEN);
        assertTrue(area.start());

        CalculatedDataDefNode cdef = new CalculatedDataDefNode();
        cdef.setName("lineDef");
        graph.addAndSaveChildren(cdef);
        cdef.setExpression("def,1,+");
        assertTrue(cdef.start());

        LineNode line = new LineNode();
        line.setName("line");
        graph.addAndSaveChildren(line);
        line.setDataDef(cdef);
        line.setWidth(1.f);
        line.setColor(GraphColor.RED);
        line.setLegend("line legend");
        assertTrue(line.start());

        cdef = new CalculatedDataDefNode();
        cdef.setName("areaDef");
        graph.addAndSaveChildren(cdef);
        cdef.setExpression("0,1,+");
        assertTrue(cdef.start());

        StackNode stack = new StackNode();
        stack.setName("stack");
        graph.addAndSaveChildren(stack);
        stack.setColor(GraphColor.GRAY);
        stack.setDataDef(cdef);
        stack.setLegend("stack legend");
        assertTrue(stack.start());


        FileUtils.writeByteArrayToFile(new File("target/graph.png"), graph.render(null, null));
    }
}