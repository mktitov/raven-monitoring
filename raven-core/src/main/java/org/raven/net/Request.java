/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import org.raven.sched.ExecutorService;

/**
 *
 * @author Mikhail Titov
 */
public interface Request {
    public final static String PROJECTS_SERVICE = "projects";
    public final static String SRI_SERVICE = "sri";
    /**
     * Returns the requester address
     */
    public String getRemoteAddr();
    /**
     * Returns the requester port
     */
    public int getRemotePort();
    /**
     * Returns server host name
     */
    public String getServerHost();
    /**
     * Returns the request headers
     */
    public Map<String, Object> getHeaders();
    /**
     * Returns mutable map of the attributes of the request 
     */
    public Map<String, Object> getAttrs();
//    /**
//     * Returns the value of the attribute or null 
//     */
//    public Object getAttr(String name);
//    /**
//     * Sets the value of the attribute
//     * @param name
//     * @param value 
//     */
//    public void setAttr(String name, Object value);
    /**
     * Returns the request parameters. It may be a query string parameters or form parameters.
     * If form parameter is a file, then the parameter type would be javax.activation.DataSource
     */
    public Map<String, Object> getParams();
    /**
     * Returns parameters with all its values
     * @return 
     */
    public Map<String, List> getAllParams();
    /**
     * Returns the service path: "sri" or "projects"
     * @return 
     */
    public String getServicePath();
    /**
     * Returns the context path. For instance for path 
     * <b>http://some.host.name/raven/sri/some/context</b> it returns <b>some/context</b>
     * @return 
     */
    public String getContextPath();
    /**Returns the raven application path in web server. For instance for path 
     * <b>http://some.host.name/raven/sri/some/context</b> it returns <b>/raven</b>
     */
    public String getRootPath();    
    /**
     * Returns http method: POST, GET, PUT or DELETE
     */
    public String getMethod();
    /**
     * Returns the value of the <b>If-Modified-Since</b> header encoded in long or -1 if value for this header
     * not assigned
     */
    public long getIfModifiedSince();
    /**
     * Returns the request content type
     */
    public String getContentType();
    /**
     * Returns the content charset
     */
    public String getContentCharset();
    /**
     * Returns the request content type or null if content type is not known
     * @throws IOException 
     */
    public InputStream getContent() throws IOException;
    /**
     * Returns the request content
     * @throws IOException 
     */
    public Reader getContentReader() throws IOException;
    /**
     * Returns the executor linked with the request
     */
    public ExecutorService getExecutor();
}
