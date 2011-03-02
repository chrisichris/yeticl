/*
 * Copyright 2011 Christian Essl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.yeticl;

import junit.framework.TestCase;

/**
 *
 * @author Christian
 */
public class YetiEvaluatorTest extends TestCase {
    
    public YetiEvaluatorTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }





    public void testEvaluteCode() throws Exception {

        YetiClassLoader ycl = new YetiClassLoader(null,null);
        YetiEvaluator ye = new YetiEvaluator(ycl);
        String r = ye.eval("2 + 2;");
        assertTrue(r.charAt(0) == '4');
    }

    public void testEvaluateCodeModuleTest() throws Exception {

        YetiClassLoader ycl = new YetiClassLoader(null,null);
        YetiEvaluator ye = new YetiEvaluator(ycl);
        //String r = ycl.eval("import java.lang.Thread; println Thread#currentThread()#getContextClassLoader()");
        String r = ye.eval("c = load org.yeticl.test;");
        assertTrue(r.startsWith("c is string = \"test\""));
    }


}
