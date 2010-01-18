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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataHandler;
import org.raven.ds.impl.AbstractAsyncDataPipe;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.IfNode;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=IfNode.class)
public class HttpSessionNode extends AbstractAsyncDataPipe
{
    public static final String IS_NEW_SESSION_BINDING = "isNewSession";
    public final static String RESPONSE = "response";
    public final static String RESPONSE_RESPONSE = "response";
    public final static String HEADERS = "headers";
    public final static String REQUEST = "request";
    public final static String REQUEST_REQUEST = "request";
    public final static String HOST = "host";
    public final static String PORT = "port";
    public final static String URI = "uri";
    public final static String PARAMS = "params";
    public final static String CONTENT = "content";
    public final static String DATA = "data";

    @Parameter
    private String username;
    
    @Parameter
    private String password;

    @Parameter
    private String domain;

    @NotNull @Parameter(defaultValue="NONE")
    private AuthSchema authSchema;

    @Parameter
    private String host;

    @Parameter(defaultValue="80")
    private Integer port;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object initRequest;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object errorHandler;

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    @Override
    public DataHandler createDataHandler()
    {
        HttpClient client = createHttpClient();
        return new HttpSessionDataHandler(client);
    }

    public AuthSchema getAuthSchema() {
        return authSchema;
    }

    public void setAuthSchema(AuthSchema authSchema) {
        this.authSchema = authSchema;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Object getInitRequest() {
        return initRequest;
    }

    public void setInitRequest(Object initRequest) {
        this.initRequest = initRequest;
    }

    public Object getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(Object errorHandler) {
        this.errorHandler = errorHandler;
    }

    private HttpClient createHttpClient()
    {
        DefaultHttpClient client = new DefaultHttpClient();
        Credentials cred = createCredentials(client);
        if (cred!=null)
            client.getCredentialsProvider().setCredentials(AuthScope.ANY, cred);

        return client;
    }

    private Credentials createCredentials(DefaultHttpClient client)
    {
        switch (authSchema)
        {
            case BASIC: return new UsernamePasswordCredentials(username, password);
            case NTLM :
                client.getAuthSchemes().register("ntlm", new NtlmSchemeFactory());
                return new NTCredentials(username, password, null, domain);
        }
        return null;
    }

    Map initRequest()
    {
        Map requestMap = new HashMap();
        requestMap.put(HEADERS, new HashMap());
        requestMap.put(PARAMS, new HashMap());
        
        bindingSupport.put(REQUEST, requestMap);
        try
        {
            getInitRequest();
        }
        finally
        {
            bindingSupport.reset();
        }

        return requestMap;
    }

    Object handleError(Map<String, Object> params)
    {
        for (Map.Entry<String, Object> param: params.entrySet())
            bindingSupport.put(param.getKey(), param.getValue());
        try
        {
            Object res = errorHandler;
            return res==null? params.get(DATA_BINDING) : res;
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    Collection<Node> getHandlers(boolean isNewSession, Object data)
    {
        try
        {
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(IS_NEW_SESSION_BINDING, isNewSession);
            return getEffectiveChildrens();
        }
        finally
        {
            bindingSupport.reset();
        }
    }
}
