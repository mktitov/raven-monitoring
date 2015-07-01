/*
 *  Copyright 2008 Mikhaile Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.raven.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.NodeAttribute;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;

/**
 *
 * @author Mikhail Titov
 */
public class LocaleReferenceValues implements AttributeReferenceValues {

    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues)
            throws TooManyReferenceValuesException 
    {
        if (!Locale.class.equals(attr.getType())) {
            return false;
        }

        Locale[] locales = Locale.getAvailableLocales();
        Arrays.sort(locales, new Comparator<Locale>() {

            public int compare(Locale o1, Locale o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        for (Locale locale : locales) {
            referenceValues.add(
                    new ReferenceValueImpl(locale.toString(), getLocaleDisplayName(locale)), null);
        }

        return true;
    }

    private String getLocaleDisplayName(Locale locale) {
        StringBuilder buf = new StringBuilder(locale.toString() + " (" + locale.getDisplayLanguage());
        if (locale.getDisplayCountry().length() > 0) {
            buf.append(", ").append(locale.getDisplayCountry());
        }
        if (locale.getDisplayVariant().length() > 0) {
            buf.append(", ").append(locale.getDisplayVariant());
        }
        buf.append(")");
        return buf.toString();
    }
}
