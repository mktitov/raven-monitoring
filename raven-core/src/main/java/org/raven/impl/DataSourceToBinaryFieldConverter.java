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
import javax.activation.DataSource;
import org.raven.ds.BinaryFieldType;
import org.raven.ds.impl.InputStreamBinaryFieldValue;
import org.weda.converter.TypeConverterException;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceToBinaryFieldConverter extends AbstractConverter<DataSource, BinaryFieldType> {

    public BinaryFieldType convert(DataSource value, Class realTargetType, String format) {
        try {
            return new InputStreamBinaryFieldValue(value.getInputStream());
        } catch (IOException ex) {
            throw new TypeConverterException(ex);
        }
    }

    public Class getSourceType() {
        return DataSource.class;
    }

    public Class getTargetType() {
        return BinaryFieldType.class;
    }
}
