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
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class HttpSessionNode extends AbstractSafeDataPipe
{
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

    @Parameter
    private Integer port;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object initRequest;

    @Override
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        Collection<Node> childs = getEffectiveChildrens();
        HttpClient client = createHttpClient();
        if (childs!=null)
        {
            HttpResponse response = null;
            for (Node child: childs)
            {
                if (child instanceof HttpResponseHandlerNode)
                {
                    Map params = new HashMap();
                    boolean isRequest = child instanceof HttpRequestNode;
                    if (isRequest)
                        params.put(REQUEST, initRequest());
                    HttpResponseHandlerNode handler = (HttpResponseHandlerNode) child;
                    Map responseMap = new HashMap();
                    responseMap.put(RESPONSE_RESPONSE, response);
                }
            }
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
                return new NTCredentials(username, password, null, host);
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
}
