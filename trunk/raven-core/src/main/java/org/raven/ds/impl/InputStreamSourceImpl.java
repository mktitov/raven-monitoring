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
package org.raven.ds.impl;

import java.io.InputStream;
import org.raven.ds.InputStreamSource;

/**
 *
 * @author Mikhail Titov
 */
public class InputStreamSourceImpl implements InputStreamSource {
    private final String path;
    private final InputStream stream;

    public InputStreamSourceImpl(String path, InputStream stream) {
        this.path = path;
        this.stream = stream;
    }

    public InputStream getInputStream() {
        return stream;
    }

    public String getPath() {
        return path;
    }
}
