/*
 *  Copyright 2008 Mikhail Titov.
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

import java.util.Locale;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class StringToLocaleConverterTest extends RavenCoreTestCase
{
	@Test
	public void converterTest() throws Exception
	{
		StringToLocaleConverter converter = new StringToLocaleConverter();
		Locale locale = converter.convert("en", Locale.class, null);
		assertNotNull(locale);
		assertEquals("en", locale.getLanguage());

		locale = converter.convert("en_CA", null, null);
		assertNotNull(locale);
		assertEquals("en", locale.getLanguage());
		assertEquals("CA", locale.getCountry());

		locale = converter.convert("ja_JP_JP", null, null);
		assertNotNull(locale);
		assertEquals("ja", locale.getLanguage());
		assertEquals("JP", locale.getCountry());
		assertEquals("JP", locale.getVariant());
	}

	@Test
	public void serviceTest() throws Exception
	{
		TypeConverter converter = registry.getService(TypeConverter.class);
		assertNotNull(converter);
		Locale locale = converter.convert(Locale.class, "en", null);
		assertNotNull(locale);
		assertEquals("en", locale.getLanguage());
	}
}