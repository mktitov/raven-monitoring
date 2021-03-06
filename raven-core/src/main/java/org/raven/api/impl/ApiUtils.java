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
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.sql.Sql;
import groovy.xml.MarkupBuilder;
import groovy.xml.MarkupBuilderHelper;
import groovyx.net.http.HTTPBuilder;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.raven.RavenUtils.*;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.DataContext;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.ListDataConsumer;
import org.raven.expr.VarsSupportState;
import org.raven.net.HttpError;
import org.raven.table.Table;
import org.raven.template.impl.TemplateNode;
import org.raven.template.impl.TemplateWizard;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.TreeImpl;
import org.raven.util.NodeUtils;
import org.weda.internal.annotations.Service;
import org.weda.internal.services.ServiceProvider;

/**
 *
 * @author Mikhail Titov
 */
public class ApiUtils {
    @Service
    private static ServiceProvider serviceProvider;
    
    public static Tree getTree(){
        return TreeImpl.INSTANCE;
    }
    
    public static<T> T getService(Class<T> serviceClass) {
        return serviceProvider.getService(serviceClass);
    }
    
    public static<T> T getService(String serviceId, Class<T> serviceClass) {
        return serviceProvider.getService(serviceId, serviceClass);
    }
    
    public static Object withConnection(Connection connection, Closure closure) throws Throwable {
        return withConnection(connection, true, closure);
    }
    
    public static Object withConnection(Connection connection, boolean autoCommit, Closure closure) throws Throwable {
        try {
            try {
                Object res = closure.call(connection);
                if (autoCommit)
                    connection.commit();
                return res;
            } catch (Throwable e) {
                if (autoCommit)
                    connection.rollback();
                throw e;
            }
        } finally {
            if (autoCommit)
                connection.close();
        }
    }

    public static Object withSql(Connection connection, Closure closure) throws Exception {
        return withSql(connection, true, closure);
    }
    
    public static Object withSql(Connection connection, boolean autoCommit, Closure closure) throws Exception {
        try {
            Sql sql = new Sql(connection);
            try{
                Object res = closure.call(sql);
                if (autoCommit)
                    connection.commit();
                return res;
            } catch(Exception e){
                if (autoCommit)
                    connection.rollback();
                throw e;
            }
        } finally {
            if (autoCommit)
                connection.close();
        }                    
    }
    //
    public static Object withHttpClient(String url, Closure closure) throws Exception {
        HTTPBuilder builder = new HTTPBuilder(url);
        builder.getClient().getParams().setParameter("http.connection.timeout", 10_000);
        builder.getClient().getParams().setParameter("http.socket.timeout", 30_000);      
        try {
            Object res = closure.call(builder);
        return res;
        } finally {
            builder.shutdown();
        }
    }
    
    public static Object catchErrors(DataContext context, Node node, Closure block) throws Throwable {
        try {
            return block.call();
        } catch (Throwable e) {
            context.addError(node, e.getCause()==null? e : e.getCause());
            throw e;
        }
    }
    
    public static Object catchErrors(DataContext context, Node node, Object finalValue, Closure block) throws Throwable {
        try {
            block.call();
        } catch (Throwable e) {
            context.addError(node, e.getCause()==null? e : e.getCause());
        }
        return finalValue;
    }
    
    public static List<Object[]> getTableRows(Table table) {
        return tableAsList(table);
    }
    
    public static List<Map<String, Object>> getTableRowsAsMap(Table table) {
        return tableRowsAsMap(table);
    }
    
    public static DataContext createDataContext() {
        return new DataContextImpl();
    }

    public static DataContext createDataContext(Map<String, Object> params) {
        DataContext ctx = new DataContextImpl();
        if (params!=null)
            for (Map.Entry<String, Object> entry: params.entrySet())
                ctx.putAt(entry.getKey(), entry.getValue());
        return ctx;
    }

    public static DataContext sendData(DataSource source, DataConsumer target, Object data) throws Exception {
        VarsSupportState varsSupportState = NodeUtils.resetVarsSupport(TreeImpl.INSTANCE);
        try {
            final DataContext context = new DataContextImpl();
            target.setData(source, data, context);
            return context;
        } finally {
            varsSupportState.restoreState();
        }
    }
    
    public static DataContext sendData(DataSource source, DataConsumer target, DataContext context, Object data) 
            throws Exception 
    {
        target.setData(source, data, context);
        return context;
    }
    
    public static List getData(Node initiator, DataSource dataSource, DataContext context) {
        VarsSupportState varsSupportState = NodeUtils.resetVarsSupport(TreeImpl.INSTANCE);
        try {
            ListDataConsumer consumer = new ListDataConsumer(initiator, context);
            dataSource.getDataImmediate(consumer, context);
            return consumer.getDataList();
        } finally {
            varsSupportState.restoreState();
        }
    }

//    public static List getData(Node initiator, DataSource dataSource) {
//        return getData(initiator, dataSource, new DataContextImpl());
//    }

    public static List<Node> createNodeFromTemplate(TemplateNode templateNode, Node destination, 
            String newNodeName, Map<String, String> vars) throws Exception
    {
        return new TemplateWizard(templateNode, destination, newNodeName, vars).createNodes();
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
    
    public static String buildXml(Closure closure) {
        final StringWriter writer = new StringWriter();
        final MarkupBuilder xml = new MarkupBuilder(writer);
        closure.call(xml);
        return writer.toString();
    }
    
    public static String buildXml(String encoding, Closure closure) {
        final StringWriter writer = new StringWriter();
        final MarkupBuilder xml = new MarkupBuilder(writer);
        MarkupBuilderHelper helper = (MarkupBuilderHelper) xml.getMkp();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "1.0");
        params.put("encoding", "encoding");
        helper.xmlDeclaration(params);
        closure.call(xml);
        return writer.toString();
    }
    
    public static Object parseJson(String jsonStr) {
        JsonSlurper parser =  new JsonSlurper();
        return parser.parseText(jsonStr);
    }
    
    public static Object parseJson(Reader jsonReader) {
        JsonSlurper parser =  new JsonSlurper();
        try {
        return parser.parse(jsonReader);
        } finally  {
//            jsonReader.close(); ???
        }
    }
    
    public static void throwHttpError(int statusCode, String contentType, String content) {
        throw new HttpError(statusCode, contentType, content);
    }
    public static void throwHttpError(int statusCode, String content) {
        throw new HttpError(statusCode, content);
    }
    public static void throwHttpError(String content) {
        throw new HttpError(content);
    }
//    public static String buildXml(Object data) {        
//        XmlBuilder json = new JsonBuilder();
//        if (data instanceof Closure)
//            json.call((Closure) data);
//        else if (data instanceof List)
//            json.call((List)data);
//        else if (data instanceof Map)
//            json.call((Map)data);
//        else
//            json.call(data);
//        return json.toString();
//    }   
}
