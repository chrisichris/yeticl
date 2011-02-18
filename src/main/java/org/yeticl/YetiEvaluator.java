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

import yeti.lang.Fun;
import yeti.lang.MList;
import yeti.lang.Struct;
import yeti.lang.compiler.YetiEval;
import yeti.lang.compiler.YeticlCompileCtxt;

/**
 *
 * @author Christian
 */
public class YetiEvaluator {
    private ClassLoader ycl;
    private final YetiEval yeval;
    private final Fun evalFun;

    public static YetiEvaluator createForSources(String[] sourcdirs) {
        return new YetiEvaluator(new YetiClassLoader(sourcdirs));
    }

    public YetiEvaluator(ClassLoader ycl){
        this.ycl = ycl;
        ClassLoader oldL = Thread.currentThread().getContextClassLoader();
        if(ycl != null)
            Thread.currentThread().setContextClassLoader(this.ycl);
        try{
            Struct struct = (Struct) YetiShellUtils.moduleRun(ycl,"yeti.lang.compiler.repl", new MList());
            this.yeval = new YetiEval();
            this.evalFun = (Fun) struct.get("evaluate");
        }catch(Exception ex) {
            throw new RuntimeException(ex);
        }finally{
            Thread.currentThread().setContextClassLoader(oldL);
        }
    }


    public String eval(String code) {
        return YeticlCompileCtxt.eval(code, evalFun, ycl, yeval);
    }

    public ClassLoader getYetiClassLoader() {
        return ycl;
    }
}
