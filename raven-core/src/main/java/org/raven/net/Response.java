/*
 * Copyright 2012 Mikhail Titov.
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

import java.nio.charset.Charset;
import java.util.Map;

/**
 *
 * @author Mikhail Titov
 */
public interface Response {
    public final static Response NOT_MODIFIED = new DummyResponse("NOT_MODIFIED");
    public final static Response ALREADY_COMPOSED = new DummyResponse("ALREADY_COMPOSED");
    public final static Response MANAGING_BY_BUILDER = new DummyResponse("MANAGING_BY_BUILDER");
    
    public String getContentType();
    public Charset getCharset();
    public Object getContent();
//    public Charset getContentEncoding();
    public Map<String, String> getHeaders();
    public Long getLastModified();
    
    static class DummyResponse implements Response {
        public final String responseType;

        public DummyResponse(String responseType) {
            this.responseType = responseType;
        }
        
        public String getContentType() {
            throw new UnsupportedOperationException("Not supported operation");
        }
        public Object getContent() {
            throw new UnsupportedOperationException("Not supported operation");
        }
        public Map<String, String> getHeaders() {
            throw new UnsupportedOperationException("Not supported operation");
        }
        public Long getLastModified() {
            throw new UnsupportedOperationException("Not supported operation");
        }
        public Charset getCharset() {
            throw new UnsupportedOperationException("Not supported operation");
        }

        @Override
        public String toString() {
            return responseType;
        }        
    }
}
