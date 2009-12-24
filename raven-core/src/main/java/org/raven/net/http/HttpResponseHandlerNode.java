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

import java.nio.charset.Charset;
import java.util.Map;
import javax.script.Bindings;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.json.JSONObject;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;
import static org.raven.net.http.HttpSessionNode.*;
/**
 *
 * @author Mikhail Titov
 */
public class HttpResponseHandlerNode extends BaseNode
{
    @Parameter
    private Charset responseContentEncoding;
    
    @NotNull @Parameter(defaultValue="TEXT")
    private ResponseContentType responseContentType;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object processResponse;

    protected BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public Object processResponse(Map<String, Object> params) throws Exception
    {
        Map<String, Object> responseMap = (Map<String, Object>) params.get(RESPONSE);
        HttpEntity entity = (HttpEntity) responseMap.get(RESPONSE_RESPONSE);
        try
        {
            Object content = entity==null? null : entity.getContent();
            if (content!=null && responseContentType!=ResponseContentType.BINARY)
            {
                Charset charset = responseContentEncoding;
                String text = IOUtils.toString(entity.getContent(), charset==null? "utf-8" : charset.name());
                content = text;
                switch (responseContentType)
                {
                    case JSON : content = new JSONObject(text); break;
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
}
