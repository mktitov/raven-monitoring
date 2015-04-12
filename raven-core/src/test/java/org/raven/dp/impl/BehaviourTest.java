/*
 * Copyright 2015 Mikhail Titov.
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
package org.raven.dp.impl;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.raven.dp.DataProcessor;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
public class BehaviourTest extends Assert {
    
    @Test
    public void andThenTest(final @Mocked DataProcessor processor) throws Exception {
        Behaviour beh = new Behaviour("test") {
            @Override public Object processData(Object dataPackage) throws Exception {
                return VOID;
            }
        };
        Behaviour composed = beh.andThen(processor);
        assertEquals(DataProcessor.VOID, composed.processData("Test"));
        new Verifications(){{
            processor.processData(any); times=0;
        }};
    }
    
    @Test
    public void andThenTest2(final @Mocked DataProcessor processor) throws Exception {
        new Expectations(){{
            processor.processData("Test"); result="Ok";
        }};
        Behaviour beh = new Behaviour("test") {
            @Override public Object processData(Object dataPackage) throws Exception {
                return UNHANDLED;
            }
        };
        Behaviour composed = beh.andThen(processor);
        assertEquals("Ok", composed.processData("Test"));
        new Verifications(){{
            processor.processData("Test"); times=1;
        }};
    }
}
