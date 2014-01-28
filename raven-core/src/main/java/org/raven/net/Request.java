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
import java.util.Map;

/**
 *
 * @author Mikhail Titov
 */
public interface Request {
    public String getRemoteAddr();
    public Map<String, Object> getHeaders();
    public Map<String, Object> getParams();
    public String getContextPath();
    public String getAppPath();
    public String getMethod();
    /**
     * Returns the value of the <b>If-Modified-Since</b> header encoded in long or -1 if value for this header
     * not assigned
     */
    public long getIfModifiedSince();
    public InputStream getContent() throws IOException;
}
