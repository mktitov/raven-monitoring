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

import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.raven.annotations.Parameter;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class HttpRequestNode extends HttpResponseHandlerNode
{
    @NotNull @Parameter(defaultValue="GET")
    private RequestType requestType;

    @NotNull @Parameter
    private String uri;

    @NotNull @Parameter(defaultValue="UTF-8")
    private String requestContentEncoding;

    @NotNull @Parameter(defaultValue="NONE")
    private RequestContentType requestContentType;

    /**
     * Returns the HttpPost or HttpGet object
     * @throws Exception
     */
    @Override
    public Object processResponse(Map<String, Object> params) throws Exception
    {
        Map<String, Object> requestMap = new HashMap<String, Object>();
        params.put(HttpSessionNode.REQUEST, requestMap);
        HttpRequestBase request = requestType==RequestType.GET? new HttpGet() : new HttpPost();
        requestMap.put(HttpSessionNode.REQUEST_REQUEST, request);

        requestMap.put(HttpSessionNode.URI, uri);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestMap.put(HttpSessionNode.HEADERS, requestHeaders);

        Map<String, String> requestParams = new HashMap<String, String>();
        requestMap.put(HttpSessionNode.PARAMS, requestParams);

        super.processResponse(params);

        for (Map.Entry<String, String> header: requestHeaders.entrySet())
            request.setHeader(header.getKey(), header.getValue());

        String _uri = (String)requestMap.get(HttpSessionNode.URI);
        if (!requestParams.isEmpty())
        {
            StringBuilder buf = new StringBuilder();
            boolean first = true;
            String encoding = requestContentEncoding;
            for (Map.Entry<String, String> param: requestParams.entrySet())
            {
                if (!first)
                    buf.append("&");
                else
                    first = false;
                buf.append(param.getKey()+"="+URLEncoder.encode(param.getValue(), encoding));
            }
            if (requestContentType == RequestContentType.NONE)
                _uri+=_uri.contains("?")? "&" : "?" + buf.toString();
            else
            {
                ((HttpPost)request).setEntity(new StringEntity(buf.toString()));
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
            }
        }
        request.setURI(new URI(_uri));
        
        return request;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getRequestContentEncoding() {
        return requestContentEncoding;
    }

    public void setRequestContentEncoding(String requestContentEncoding) {
        this.requestContentEncoding = requestContentEncoding;
    }

    public RequestContentType getRequestContentType() {
        return requestContentType;
    }

    public void setRequestContentType(RequestContentType requestContentType) {
        this.requestContentType = requestContentType;
    }
}
