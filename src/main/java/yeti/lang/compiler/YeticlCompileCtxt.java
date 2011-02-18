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

package yeti.lang.compiler;

import java.io.File;
import org.yeticl.FileSourceReader;
import yeti.lang.Fun;
import yeti.lang.IntNum;

/**
 *
 * @author Christian
 */
public class YeticlCompileCtxt {
    private final CompileCtx ctx;


    public static void compileAll(String[] classPath,String[] baseDirs,String[] sources,String destDir ) throws Exception {
        if(!(destDir.endsWith("/") || destDir.endsWith(File.separator))){
            destDir = destDir + File.separator;
        }

        String[] preload = new String[]{"yeti/lang/std","yeti/lang/io"};
        CodeWriter codeWriter = new ToFile(destDir);
        ClassFinder classFinder = new ClassFinder(classPath);
        FileSourceReader reader = new FileSourceReader(baseDirs);
        //YetiC reader = new YetiC();
        //reader.basedirs = baseDirs;
        CompileCtx ctxt = new CompileCtx(reader,codeWriter,preload,classFinder);
        CompileCtx old = CompileCtx.current();
        CompileCtx.currentCompileCtx.set(ctxt);
        try{
            ctxt.compileAll(sources, 0, new String[]{});
        }finally{
            CompileCtx.currentCompileCtx.set(old);
        }

    }

    public YeticlCompileCtxt(SourceReader reader, CodeWriter writer, String[] preload) {
        this.ctx = new CompileCtx(reader,writer,preload, new ClassFinder(new String[]{}));
    }

    public String compile(String sourceName, int flags) throws Exception {
        CompileCtx old = CompileCtx.current();
        CompileCtx.currentCompileCtx.set(ctx);
        try{

            return ctx.compile(sourceName,flags);
        }finally{
            CompileCtx.currentCompileCtx.set(old);
        }
    }

    public static String eval(String code, Fun evalFun,ClassLoader ycl,YetiEval yeval) {
        ClassLoader oldL = Thread.currentThread().getContextClassLoader();
        if(ycl != null)
            Thread.currentThread().setContextClassLoader(ycl);
        YetiEval.set(yeval);
        CompileCtx oldC = CompileCtx.current();
        try{
            Fun f2 = (Fun) evalFun.apply(code);
            return (String) f2.apply(new IntNum(0));
        }catch(CompileException ex){
            //ex.printStackTrace();;
            System.out.println("Error compiling:"+ex.getMessage());
            return "Error compiling:" + ex.getMessage();
        }finally{
            CompileCtx.currentCompileCtx.set(oldC);
            Thread.currentThread().setContextClassLoader(oldL);
        }

    }

}
