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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import yeti.lang.Fun;

/**
 *
 * @author Christian
 */
public class YetiShellUtils {

    public static boolean USE_JLINE = false;
    static {
        try{
            YetiShellUtils.class.getClassLoader().loadClass("jline.ConsoleReader");
            USE_JLINE = true;
        }catch(Exception x){
            USE_JLINE = false;
        }
    }

    static public Object moduleLoad(ClassLoader classLoader,String moduleName) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        classLoader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            Class cl = classLoader.loadClass(moduleName);
            Method evalM = cl.getMethod("eval", new Class[]{});
            return evalM.invoke(null, new Object[]{});
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    static public Object moduleRun(ClassLoader classLoader,String moduleName, Object param) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        if(classLoader != null)
            Thread.currentThread().setContextClassLoader(classLoader);
        try {
            Fun fun = (Fun) moduleLoad(classLoader,moduleName);
            return fun.apply(param);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    static public String eval(String yetiCode) {
        YetiClassLoader cl = new YetiClassLoader(null,null);
        YetiEvaluator ye = new YetiEvaluator(cl);
        return ye.eval(yetiCode);
    }

    public static ThreadLocal EVAL_RESULT = new ThreadLocal();

    static public void setEvalResult(Object value) {
        EVAL_RESULT.set(value);
    }

    static public Object evalWithResult(String yetiCode) {
        YetiClassLoader cl = new YetiClassLoader(null,null);
        YetiEvaluator ye = new YetiEvaluator(cl);
        yetiCode = "result = ("+yetiCode+")";
        ye.eval("YetiShellUtils#setEvalResult(result);");
        return EVAL_RESULT.get();
    }


  

    private static final ThreadLocal SHELL_EVALUATOR = new ThreadLocal();

    public static YetiEvaluator getShellEvaluator() {
        return (YetiEvaluator) SHELL_EVALUATOR.get();
    }

    private static final ThreadLocal SHELL_SOURCE_DIRS = new ThreadLocal();
    public static String[] getShellSourceDirs() {
        return (String[]) SHELL_SOURCE_DIRS.get();
    }

 

    public static String evaluateLoop(ClassLoader parent, String[] sourceDirs, String[] commands) {
        //prepare the sourcedirs

        boolean cont = true;
        while(cont) {
            YetiClassLoader ld = sourceDirs == null || sourceDirs.length == 0 ?
                                    new YetiClassLoader(parent, null) :
                                    new YetiClassLoader(parent,new FileSourceReader(sourceDirs));
            
            YetiEvaluator shellEv = new YetiEvaluator(ld);
            SHELL_EVALUATOR.set(shellEv);
            SHELL_SOURCE_DIRS.set(sourceDirs == null ? new String[]{} : sourceDirs);

            shellEv.eval("s = load org.yeticl.shell");
            System.out.println("----------------------------------------");
            System.out.println("Welcome to yeti-shell, enter -h for help");
            System.out.println("----------------------------------------\n");

            if(commands != null) {
                System.out.println("executing init commands:\n");
                for(int i=0;i<commands.length;i++) {
                    String cd = commands[i];
                    System.out.println("init-command>"+cd);
                    System.out.println(shellEv.eval(cd));
                }
                System.out.println("--------------------------\n");

            }
            
            String ret = shellEv.eval("s._start ()");
            cont = ret.startsWith("\"restart\"");
        }

        return "quited";
    }

}
