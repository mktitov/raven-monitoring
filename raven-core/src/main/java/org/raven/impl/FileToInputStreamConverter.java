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
package org.raven.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class FileToInputStreamConverter extends AbstractConverter<File, InputStream> {

    public InputStream convert(File value, Class realTargetType, String format) {
        try {
            return new FileInputStream(value);
        } catch (FileNotFoundException ex) {
            throw new TypeConverterException(ex);
        }
    }

    public Class getSourceType() {
        return File.class;
    }

    public Class getTargetType() {
        return InputStream.class;
    }
}
