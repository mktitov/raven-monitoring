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

package org.raven.ds.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.impl.objects.TestDataConsumer2;
import org.raven.rrd.data.DataAndTime;
import org.raven.PushDataSource;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class DataAndTimeGeneratorTest extends RavenCoreTestCase
{
	@Test
	public void test() throws ParseException
	{
		PushDataSource ds = new PushDataSource();
		ds.setName("ds");
		tree.getRootNode().addChildren(ds);
		ds.save();
		ds.init();
		ds.start();
		assertEquals(Node.Status.STARTED, ds.getStatus());

		DataAndTimeGenerator gen = new DataAndTimeGenerator();
		gen.setName("gen");
		tree.getRootNode().addChildren(gen);
		gen.save();
		gen.init();
		gen.setDataSource(ds);
		gen.setLocale(Locale.ENGLISH);
		gen.setDate("01-OCT-2008 00:00:00");
		gen.setDatePattern("dd-MMM-yyyy HH:mm:ss");
		gen.start();
		assertEquals(Node.Status.STARTED, gen.getStatus());

		TestDataConsumer2 consumer = new TestDataConsumer2();
		consumer.setName("consumer");
		tree.getRootNode().addChildren(consumer);
		consumer.save();
		consumer.init();
		consumer.setDataSource(gen);
		consumer.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.DONT_RESET_DATA);
		consumer.start();
		assertEquals(Node.Status.STARTED, consumer.getStatus());

		ds.pushData(1);
		Object data = consumer.getData();
		assertNotNull(data);
		assertTrue(data instanceof DataAndTime);
		SimpleDateFormat fmt = new SimpleDateFormat(gen.getDatePattern(), Locale.ENGLISH);
		Date date = fmt.parse(gen.getDate());
		long longDate = date.getTime()/1000;
		DataAndTime dataAndTime = (DataAndTime) data;
		assertEquals(1, dataAndTime.getData());
		assertEquals(longDate, dataAndTime.getTime());
	}

	@Test
	@Ignore
	public void localesList() throws Exception
	{
		Locale[] locales = Locale.getAvailableLocales();
		Arrays.sort(locales, new Comparator<Locale>() {
			public int compare(Locale o1, Locale o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
		for (Locale locale: locales)
			System.out.println(locale.toString());
		fail();

	}
}