/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.net.impl;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.log.LogLevel;
import org.raven.net.AccessDeniedException;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.RequiredParameterMissedException;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseContextNodeTest extends RavenCoreTestCase
{
    private PushOnDemandDataSource ds;
    private NetworkResponseContextNode context ;

    @Before
    public void prepare() {
        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        context = new NetworkResponseContextNode();
        context.setName("context");
        tree.getRootNode().addAndSaveChildren(context);
        context.setLogLevel(LogLevel.DEBUG);
        context.setDataSource(ds);
    }

    @Test(expected=AccessDeniedException.class)
    public void accessDeniedTest() throws NetworkResponseServiceExeption {
        assertTrue(context.start());
        context.getResponse("1.1.1.1", null);
    }

    @Test
    public void allowRequestsFromAnyIpTest() throws NetworkResponseServiceExeption
    {
        context.setAllowRequestsFromAnyIp(true);
        context.getResponse("1.1.1.1", null);
    }

    @Test
    public void accessAllowedTest() throws NetworkResponseServiceExeption
    {
        assertTrue(context.start());
        AddressListNode addressList = context.getAddressListNode();
        SimpleAddressMatcherNode matcher = new SimpleAddressMatcherNode();
        matcher.setName("1.1.1.1");
        addressList.addAndSaveChildren(matcher);
        assertTrue(matcher.start());

        context.getResponse("1.1.1.1", null);
    }

    @Test(expected=RequiredParameterMissedException.class)
    public void missedRequiredParameterTest() throws NetworkResponseServiceExeption
    {
        context.setAllowRequestsFromAnyIp(true);
        assertTrue(context.start());

        ParameterNode parameter = new ParameterNode();
        parameter.setName("param");
        context.getParametersNode().addAndSaveChildren(parameter);
        parameter.setRequired(true);
        parameter.setParameterType(String.class);
        assertTrue(parameter.start());

        context.getResponse("1.1.1.1", null);
    }

    @Test(expected=RequiredParameterMissedException.class)
    public void missedRequiredParameterTest2() throws NetworkResponseServiceExeption
    {
        context.setAllowRequestsFromAnyIp(true);
        assertTrue(context.start());

        ParameterNode parameter = new ParameterNode();
        parameter.setName("param");
        context.getParametersNode().addAndSaveChildren(parameter);
        parameter.setRequired(true);
        parameter.setParameterType(String.class);
        assertTrue(parameter.start());

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("param", null);
        context.getResponse("1.1.1.1", params);
    }

    @Test
    public void paramsTest() throws NetworkResponseServiceExeption
    {
        context.setAllowRequestsFromAnyIp(true);
        assertTrue(context.start());

        ParameterNode parameter = new ParameterNode();
        parameter.setName("param");
        context.getParametersNode().addAndSaveChildren(parameter);
        parameter.setRequired(true);
        parameter.setParameterType(Integer.class);
        assertTrue(parameter.start());

        ds.addDataPortion("test");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("param", "1");
        assertEquals("test", context.getResponse("1.1.1.1", params).getContent());
    }

    @Test
    public void getResponseTest() throws NetworkResponseServiceExeption
    {
        context.setAllowRequestsFromAnyIp(true);
        assertTrue(context.start());

        ds.addDataPortion("test");
        assertEquals("test", context.getResponse("1.1.1.1", null).getContent());
    }
}