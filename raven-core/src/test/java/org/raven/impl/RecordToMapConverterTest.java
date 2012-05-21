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

import java.util.Map;
import static org.easymock.EasyMock.*;
import org.junit.Test;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class RecordToMapConverterTest extends RavenCoreTestCase {
    private Map map;
    private Record record;
    
    @Test
    public void instanceTest() throws RecordException {
        createMocks();
        RecordToMapConverter converter = new RecordToMapConverter();
        assertSame(map, converter.convert(record, null, null));
        verifyMocks();
    }
    
    @Test
    public void serviceTest() throws RecordException {
        createMocks();
        TypeConverter converter = registry.getService(TypeConverter.class);
        assertSame(map, converter.convert(Map.class, record, null));
        verifyMocks();
    }
    
    private void createMocks() throws RecordException {
        record = createMock(Record.class);
        map = createMock(Map.class);
        expect(record.getValues()).andReturn(map);
        replay(record, map);
    }
    
    private void verifyMocks() {
        verify(record, map);
    }
}
