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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.script.Bindings;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataHandler;
import org.raven.ds.impl.AbstractAsyncDataPipe;
import org.raven.expr.impl.IfNode;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.Node;
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
    public final static String STOP_PROCESSING = "STOP_PROCESSING";
    public final static String STOP_PROCESSING_BINDING="STOP_PROCESSING";

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

    @Parameter(defaultValue="5000")
    private Integer connectionTimeout;

    @NotNull @Parameter(defaultValue="10")
    private Integer maxPercentOfErrors;

    @NotNull @Parameter(defaultValue="10")
    private Integer checkMaxPercentOfErrorsAfter;

    private AtomicLong requestSetNumber;

    @Override
    protected void initFields() {
        super.initFields();
        requestSetNumber = new AtomicLong(0);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        requestSetNumber.set(0);
    }

    long getNextRequestSetNumber(){
        return requestSetNumber.incrementAndGet();
    }

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

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
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

    public Integer getCheckMaxPercentOfErrorsAfter() {
        return checkMaxPercentOfErrorsAfter;
    }

    public void setCheckMaxPercentOfErrorsAfter(Integer checkMaxPercentOfErrorsAfter) {
        this.checkMaxPercentOfErrorsAfter = checkMaxPercentOfErrorsAfter;
    }

    public Integer getMaxPercentOfErrors() {
        return maxPercentOfErrors;
    }

    public void setMaxPercentOfErrors(Integer maxPercentOfErrors) {
        this.maxPercentOfErrors = maxPercentOfErrors;
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

    Collection<Node> getHandlers(boolean isNewSession, Object data, DataContext context)
    {
        try
        {
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(IS_NEW_SESSION_BINDING, isNewSession);
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            return getEffectiveChildrens();
        }
        finally
        {
            bindingSupport.reset();
        }
    }
}
