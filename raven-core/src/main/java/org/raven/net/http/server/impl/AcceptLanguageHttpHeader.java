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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author Mikhail Titov
 */
public class AcceptLanguageHttpHeader {
    private static final String INVALID__ACCEPT_LANGUAGE_HEADER_VALUE = "Invalid Accept-Language header value: ";
    private final List<Locale> locales;
    
    public AcceptLanguageHttpHeader(String headerValue) {
        if (headerValue==null)
            throw new NullPointerException("Parameter headerValue can not be null");
        String[] langDefs = headerValue.split(",");        
        if (langDefs.length==0)
            locales = Collections.EMPTY_LIST;
        else {
            List<LocaleSlot> slots = new ArrayList<>(langDefs.length);
            for (String langDef: headerValue.split(",")) {
                //may be in form: la[-la][;q=D]
                String[] defElems = langDef.split(";");
                if (defElems.length==0)
                    throw new IllegalArgumentException(INVALID__ACCEPT_LANGUAGE_HEADER_VALUE+headerValue);
                String localeStr =defElems[0].trim();
                if ("*".equals(localeStr))
                    continue;            
                Locale locale = Locale.forLanguageTag(localeStr);
                double q = 1.0;
                if (defElems.length>1 && defElems[1].trim().startsWith("q=")) {
                    String[] qElems = defElems[1].split("=");
                    if (qElems.length!=2)
                        throw new IllegalArgumentException(INVALID__ACCEPT_LANGUAGE_HEADER_VALUE+headerValue);
                    try {
                        q = Double.parseDouble(qElems[1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(INVALID__ACCEPT_LANGUAGE_HEADER_VALUE+headerValue, e);
                    }
                }
                slots.add(new LocaleSlot(locale, q));
            }
            Collections.sort(slots);
            locales = new ArrayList<>(slots.size());
            for (LocaleSlot slot: slots)
                locales.add(slot.locale);
        }
    }
    
    public Locale getLocale() {
        return locales.isEmpty()? null : locales.get(0);
    }

    public List<Locale> getLocales() {
        return locales;
    }
    
    private static class LocaleSlot implements Comparable<LocaleSlot> {
        private final Locale locale;
        private final double q;

        public LocaleSlot(Locale locale, double q) {
            this.locale = locale;
            this.q = q;
        }        
        
        @Override
        public int compareTo(LocaleSlot t) {            
            return t==null? -1 : Double.compare(t.q, q); //reverse sort
        }
    }
}
