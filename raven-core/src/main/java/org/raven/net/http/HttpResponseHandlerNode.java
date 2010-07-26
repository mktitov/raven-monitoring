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

package org.raven.net.http;

import groovy.util.XmlSlurper;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import javax.script.Bindings;
import net.sf.json.groovy.JsonSlurper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.ccil.cowan.tagsoup.Parser;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.impl.BaseNode;
import org.raven.util.OperationStatistic;
import org.weda.annotations.constraints.NotNull;
import static org.raven.net.http.HttpSessionNode.*;
/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=HttpSessionNode.class)
public class HttpResponseHandlerNode extends BaseNode
{

    public final static String PROCESS_RESPONSE_ATTR = "processResponse";
    @Parameter
    private Charset responseContentEncoding;
    
    @NotNull @Parameter(defaultValue="TEXT")
    private ResponseContentType responseContentType;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object processResponse;

    @Parameter(defaultValue="200")
    private Integer expectedResponseStatusCode;

    @Parameter(readOnly=true)
    protected OperationStatistic operationStatistic;

    protected BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        operationStatistic = new OperationStatistic();
    }

    public Object processResponse(Map<String, Object> params) throws Exception
    {
        Map<String, Object> responseMap = (Map<String, Object>) params.get(RESPONSE);
        HttpResponse response = (HttpResponse) responseMap.get(RESPONSE_RESPONSE);
        HttpEntity entity = response==null? null : response.getEntity();
        try
        {
            InputStream contentStream = entity==null? null : entity.getContent();
            Object content = contentStream;
            if (content!=null && responseContentType!=ResponseContentType.BINARY)
            {
                switch (responseContentType)
                {
                    case HTML:
                    case TEXT : 
                    case JSON :
                        Charset charset = responseContentEncoding;
                        String text = IOUtils.toString(contentStream, charset==null? "utf-8" : charset.name());
                        if (isLogLevelEnabled(LogLevel.TRACE))
                            trace("RESPONSE CONTENT: "+text);
                        switch (responseContentType)
                        {
                            case TEXT: content = text; break;
                            case JSON: content = new JsonSlurper().parseText(text); break;
                            case HTML: content = new XmlSlurper(new Parser()).parseText(text); break;
                        }
                        break;
                    case XML :
                        content = new XmlSlurper().parse(contentStream);
                }
            }
            responseMap.put(CONTENT, content);

            for (Map.Entry<String, Object> entry: params.entrySet())
                bindingSupport.put(entry.getKey(), entry.getValue());

            return processResponse;
        }
        finally
        {
            bindingSupport.reset();
            if (entity!=null)
                entity.consumeContent();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
    
    public OperationStatistic getOperationStatistic() {
        return operationStatistic;
    }

    public Object getProcessResponse() {
        return processResponse;
    }

    public void setProcessResponse(Object processResponse) {
        this.processResponse = processResponse;
    }

    public Charset getResponseContentEncoding() {
        return responseContentEncoding;
    }

    public void setResponseContentEncoding(Charset responseContentEncoding) {
        this.responseContentEncoding = responseContentEncoding;
    }

    public ResponseContentType getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(ResponseContentType responseContentType) {
        this.responseContentType = responseContentType;
    }
    
    public Integer getExpectedResponseStatusCode() {
        return expectedResponseStatusCode;
    }

    public void setExpectedResponseStatusCode(Integer expectedResponseStatusCode) {
        this.expectedResponseStatusCode = expectedResponseStatusCode;
    }
}
