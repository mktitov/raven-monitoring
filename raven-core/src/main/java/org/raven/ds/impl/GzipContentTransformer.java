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

package org.raven.ds.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.raven.annotations.NodeClass;
import org.raven.net.ContentTransformer;
import org.raven.net.Outputable;
import org.raven.net.impl.FileResponseBuilder;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=FileResponseBuilder.class)
public class GzipContentTransformer extends BaseNode implements ContentTransformer {

    public Outputable transform(final InputStream source, Map bindings, Charset charset) {
        return new Outputable() {
            public OutputStream outputTo(OutputStream out) throws IOException {
                GZIPOutputStream gzip = new GZIPOutputStream(out);
                try {
                    IOUtils.copy(source, gzip);
                    return out;
                } finally {
                    gzip.finish();                    
                }
            }
        };
    }

    public Charset getResultCharset() {
        return null;
    }
}
