/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.net.http.server.impl;

import java.util.Locale;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class AcceptLanguageHttpHeaderTest {
    @Test
    public void test() {
        AcceptLanguageHttpHeader h = new AcceptLanguageHttpHeader("ru-RU,ru;q=0.8,en-US;q=0.1,en;q=0.4,fr;q=0.2");
        assertNotNull(h.getLocale());
        assertEquals(new Locale("ru","ru"), h.getLocale());
     
        assertEquals(5, h.getLocales().size());
        assertEquals(Locale.US, h.getLocales().get(4));
    }
    
    @Test
    public void test2() {
        AcceptLanguageHttpHeader h = new AcceptLanguageHttpHeader("ru;q=0.8,en-US;q=0.1,en;q=0.4,fr;q=0.2,ru-RU;q=0.7");
        assertNotNull(h.getLocale());
        assertEquals(new Locale("ru"), h.getLocale());
     
        assertEquals(5, h.getLocales().size());
        assertEquals(Locale.US, h.getLocales().get(4));
    }
}
