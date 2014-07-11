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

package org.raven.ui.servlet;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.RequestContext;

/**
 *
 * @author Mikhail Titov
 */
public class FileUploadContext implements RequestContext {
    private final HttpServletRequest request;
    private final CometInputStream stream;

    public FileUploadContext(HttpServletRequest request) throws IOException {
        this.request = request;
        this.stream = new CometInputStream();
    }

    public String getCharacterEncoding() {
        return request.getCharacterEncoding();
    }

    public String getContentType() {
        return request.getContentType();
    }

    public int getContentLength() {
        return request.getContentLength();
    }

    public InputStream getInputStream() throws IOException {
        return stream;
    }
    
    public CometInputStream getCometInputStream() {
        return stream;
    }
}
