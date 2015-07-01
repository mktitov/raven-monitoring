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

package org.raven.impl;

import java.io.IOException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypes;
import org.raven.MimeTypeService;

/**
 *
 * @author Mikhail Titov
 */
public class MimeTypeServiceImpl implements MimeTypeService {

    public MimeTypeServiceImpl() {
    }

    public String getContentType(String filename) {
        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, filename);
        try {
            return MimeTypes.getDefaultMimeTypes().detect(null, metadata).toString();
        } catch (IOException ex) {
            return MimeTypes.OCTET_STREAM;
        }
    }
}
