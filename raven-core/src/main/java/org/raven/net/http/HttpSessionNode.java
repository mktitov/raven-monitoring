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

import java.sql.ClientInfoStatus;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class HttpSessionNode extends AbstractSafeDataPipe
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

    @Parameter
    private Boolean keepAliveSession;

    @Parameter
    private Integer keepAliveSessionTime;

    private List<SessionInfo> sessions;
    private ReadWriteLock sessionLock;
    private ThreadLocal<HttpClient> httpClient;
    private Lock httpClientLock;
    private long httpClientCreationTime;

    @Override
    protected void initFields()
    {
        super.initFields();

        sessionLock = new ReentrantReadWriteLock();

        httpClientLock = new ReentrantLock();
        httpClient = null;
        httpClientCreationTime = 0;
        httpClient = new ThreadLocal<HttpClient>();
    }


    @Override
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        boolean isNewSession = false;
        HttpClient client = null;
        if (keepAliveSession)
            client = httpClient.get();
        if (client==null)
        {
            isNewSession = true;
            client = createHttpClient();
            if (keepAliveSession)
                httpClient.set(client);
        }
        
        Collection<Node> childs = getEffectiveChildrens();
        if (childs!=null)
        {
            HttpResponse response = null;
            Object res = null;
            for (Node child: childs)
            {
                if (child instanceof HttpResponseHandlerNode)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put(DATA_BINDING, data);
                    params.put(SKIP_DATA_BINDING, SKIP_DATA);
                    params.put(IS_NEW_SESSION_BINDING, isNewSession);
                    HttpResponseHandlerNode handler = (HttpResponseHandlerNode) child;

                    Boolean handlerEnabled = handler.getEnabled();
                    if (handlerEnabled==null || !handlerEnabled)
                        continue;

                    boolean isRequest = child instanceof HttpRequestNode;
                    if (isRequest)
                        params.put(REQUEST, initRequest());


                    Map responseMap = new HashMap();
                    responseMap.put(RESPONSE_RESPONSE, response);
                    params.put(RESPONSE, responseMap);

                    Integer expectedStatus = handler.getExpectedResponseStatusCode();
                    if (response!=null && expectedStatus!=null
                        && !expectedStatus.equals(response.getStatusLine().getStatusCode()))
                    {
                        handleError(params);
                        return;
                    }

                    res = handler.processResponse(params);

                    if (isRequest)
                    {
                        Map requestMap = (Map)params.get(REQUEST);
                        HttpRequest request = (HttpRequest)requestMap.get(REQUEST_REQUEST);
                        HttpHost target =
                                new HttpHost((String)requestMap.get(HOST), (Integer)requestMap.get(PORT));
                        response = client.execute(target, request);
                    }
                }
            }
            if (SKIP_DATA!=res)
                sendDataToConsumers(res==null? data : res);
        }
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

    public Boolean getKeepAliveSession() {
        return keepAliveSession;
    }

    public void setKeepAliveSession(Boolean keepAliveSession) {
        this.keepAliveSession = keepAliveSession;
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

    private Map initRequest()
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

    private void handleError(Map<String, Object> params)
    {
        for (Map.Entry<String, Object> param: params.entrySet())
            bindingSupport.put(param.getKey(), param.getValue());
        try
        {
            Object res = errorHandler;
            if (SKIP_DATA!=res)
                sendDataToConsumers(res==null? params.get(DATA_BINDING) : res);
        }
        finally
        {
            bindingSupport.reset();
        }
    }

//    private SessionInfo getSessionInfo()
//    {
//
//    }
//
    private class SessionInfo
    {
        private DefaultHttpClient client;
        private long clientCreationTime;
        private boolean busy = false;

        public boolean isBusy() {
            return busy;
        }

        public boolean isHttpClientInitialized()
        {
            return client!=null;
        }

        public HttpClient getHttpClient()
        {
            long keepAliveTime = keepAliveSessionTime==null? Integer.MAX_VALUE : keepAliveSessionTime;
            if (client==null || (System.currentTimeMillis()-keepAliveTime*1000)>clientCreationTime)
            {
                clientCreationTime = System.currentTimeMillis();
                client = new DefaultHttpClient();
                Credentials cred = createCredentials(client);
                if (cred!=null)
                    client.getCredentialsProvider().setCredentials(AuthScope.ANY, cred);
            }
            busy = true;
            
            return client;
        }

        public void freeHttpClient()
        {
            busy = false;
        }
    }
}
