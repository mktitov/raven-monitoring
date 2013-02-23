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

package org.raven.api.impl;

import groovy.json.JsonBuilder;
import groovy.lang.Closure;
import groovy.sql.Sql;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.raven.RavenUtils;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.ListDataConsumer;
import org.raven.table.Table;
import org.raven.template.impl.TemplateNode;
import org.raven.template.impl.TemplateWizard;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.TreeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class ApiUtils
{
    public static Tree getTree(){
        return TreeImpl.INSTANCE;
    }

    public static void withConnection(Connection connection, Closure closure) throws SQLException {
        try {
            closure.call(connection);
        } finally {
            connection.close();
        }
    }

    public static Object withSql(Connection connection, Closure closure) throws Exception {
        try {
            Sql sql = new Sql(connection);
            try{
                Object res = closure.call(sql);
                connection.commit();
                return res;
            } catch(Exception e){
                connection.rollback();
                throw e;
            }
        } finally {
            connection.close();
        }
    }
    
    public static List<Object[]> getTableRows(Table table) {
        return RavenUtils.tableAsList(table);
    }
    
    public static DataContext createDataContext() {
        return new DataContextImpl();
    }

    public static void sendData(DataSource source, DataConsumer target, Object data) throws Exception {
        target.setData(source, data, new DataContextImpl());
    }
    
    public static List getData(Node initiator, DataSource dataSource, DataContext context) {
        ListDataConsumer consumer = new ListDataConsumer(initiator, context);
        dataSource.getDataImmediate(consumer, context);
        return consumer.getDataList();
    }

    public static void createNodeFromTemplate(TemplateNode templateNode, Node destination, String newNodeName
            , Map<String, String> vars) throws Exception
    {
        new TemplateWizard(templateNode, destination, newNodeName, vars).createNodes();
    }
    
    public static String buildJson(Object data) {        
        JsonBuilder json = new JsonBuilder();
        if (data instanceof Closure)
            json.call((Closure) data);
        else if (data instanceof List)
            json.call((List)data);
        else if (data instanceof Map)
            json.call((Map)data);
        else
            json.call(data);
        return json.toString();
    }
    
//    public static Closure wrapNode(Object nodeObj, Closure closure) {
//        if (nodeObj==null)
//            return null;
//        NodeAccess node = null;
//        if (nodeObj instanceof Node)
//            node = new NodeAccessImpl((Node)nodeObj);
//        else if (nodeObj instanceof NodeAccess)
//            node = (NodeAccess) nodeObj;
//        else
//            throw new IllegalArgumentException("Invalid type of nodeObj. Expected Node or NodeAccess");
//        closure.setDelegate(new PropertySupport(closure, node));
//        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
//        return closure;
//    }
    
//    public static Object invokeMissingMethod(Object obj, NodeAccess node, String name, Object args) {
//        Object[] list = (Object[]) args;
//        if (list.length==1 && list[0] instanceof Map) {
//            NodeAttributeAccess attr = node.getAt(name);
//            if (attr!=null) 
//                return attr.getValue((Map)list[0]);
//        }
//        throw new MissingMethodException(name, obj.getClass(), list);
//    }
    
//    public static Object getMissingProperty(NodeAccess node, String name) {
//        NodeAttributeAccess attr = node.getAt(name);
//        if (attr!=null)
//            return attr.getValue();
//        else
//            throw new MissingPropertyException(name);
//    }
    
//    public static Object getMissingProperty(String name, Map props) {
//        NodeAccess node = (NodeAccess) props.get("node");
//        if (node!=null) {
//            NodeAttributeAccess attr = node.getAt(name);
//            if (attr!=null)
//                return attr.getValue();
//        }
//        return props.get(name);
//    }
    
//    public static Object[] getMissingProperty(NodeAccess node, String name, Map props) {
//        NodeAttributeAccess attr = node.getAt(name);
//        return attr==null? null : new Object[]{attr.getValue()};
//    }
    
//    public static Object setMissingProperty(String name, Object value, Map props) {
//        NodeAccess node = (NodeAccess) props.get("node");
//        if (node!=null) {
//            NodeAttribute attr = node.asNode().getNodeAttribute(name);
//            if (attr!=null) 
//                try {
//                    attr.setValue(value==null? null : value.toString());
//                    attr.save();
//                    return attr.getRealValue();
//                } catch (Exception e) {
//                    throw new java.lang.IllegalArgumentException(e);
//                }
//        }
//        props.put(name, value);
//        return value;
//    }
}
