/*
 * Copyright 2014 Mikhail Titov.
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

package org.raven.net.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.raven.net.CometRequest;

/**
 *
 * @author Mikhail Titov
 */
public class CommetRequestImpl extends RequestImpl implements CometRequest {

    public CommetRequestImpl(String remoteAddr, Map<String, Object> params, Map<String, Object> headers, 
            String contextPath, String method, HttpServletRequest httpRequest) 
    {
        super(remoteAddr, params, headers, contextPath, method, httpRequest);
    }

    public void processReadOperation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InputStream getContent() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
        
    }

    @Override
    public Reader getContentReader() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
