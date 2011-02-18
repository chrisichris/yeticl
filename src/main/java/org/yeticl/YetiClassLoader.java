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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import yeti.lang.compiler.CodeWriter;
import yeti.lang.compiler.SourceReader;
import yeti.lang.compiler.YeticlCompileCtxt;

/**
 *
 * @author Christian
 */
public class YetiClassLoader extends ClassLoader implements CodeWriter {

    private final Map classes = new HashMap(); //stores the compile classes
    private final SourceReader pathSources;
    private final YeticlCompileCtxt compileCtx;
    private final ClassResourceClassLoader compileClassLoader; //classloader used for comilation
    
    public YetiClassLoader(ClassLoader parent, SourceReader delegate) {
        super(parent == null ? Thread.currentThread().getContextClassLoader() : parent);
        if (parent == null) {
            parent = Thread.currentThread().getContextClassLoader();
        }
        //read sources and classes for the compiler only from parent
        PathSourceReader pr = new PathSourceReader(parent);
        SourceReader tr = delegate == null ? pr : new DelegateSourceReader(delegate,pr);
        this.compileClassLoader = new ClassResourceClassLoader(parent);
        this.pathSources = new ParentLimitedSourceReader(tr, compileClassLoader);
        this.compileCtx = new YeticlCompileCtxt(pathSources, this, new String[]{"yeti/lang/std", "yeti/lang/io"});

    }
    public YetiClassLoader(String[] sourcedirs) {
        this(null, sourcedirs == null ? null : new FileSourceReader(sourcedirs));
    }

    

    public synchronized void writeClass(String name, byte[] code) {
        // to a dotted classname used by loadClass
        classes.put(name.substring(0, name.length() - 6).replace('/', '.'),
                code);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
        return super.loadClass(name);
    }


    //invoked by super.loadClass
    protected Class findClass(String name) throws ClassNotFoundException {
        byte[] code = (byte[]) classes.get(name);
        if (code != null) {
            return defineClass(name, code, 0, code.length);
        }

        String flName = name.replace('.', '/') + ".yeti";

        
        try {
            pathSources.getSource(new String[]{flName}, false);
        } catch (Exception ex) {
            throw new ClassNotFoundException(name);
        }

        compile(flName);
        code = (byte[]) classes.get(name);
        if (code == null) {
            throw new ClassNotFoundException(name + " or " + flName);
        } else {
            return defineClass(name, code, 0, code.length);
        }
    }

    public InputStream getResourceAsStream(String path) {
        InputStream superStream = super.getResourceAsStream(path);
        if(superStream != null || (!path.endsWith("class"))) {
            return superStream;
        }
        String name = path.substring(0, path.length() - 6).replace('/', '.');
        byte[] code = (byte[]) classes.get(name);

        if (code == null) {
            //make sure the class exists
            try {
                loadClass(name);
            } catch (ClassNotFoundException ex) {
                return null;
            }
            code = (byte[]) classes.get(name);
        }
        if (code != null) {
            return new ByteArrayInputStream(code);
        }

        return null;
    }

    private void compile(String name) throws ClassNotFoundException {
        //use the parent ClassLoader as context ClassLoader because compiler will look inthis one for needed classes
        //throguh getResourceAsStream(type.class). Because some classloaders do not delegate to parent
        //we wrap this in a special class loader which will do so.
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(compileClassLoader);
        YeticlCompileCtxt compileCtx = new YeticlCompileCtxt(pathSources, this, new String[]{"yeti/lang/std", "yeti/lang/io"});
        try {
            compileCtx.compile(name, 0);
        }catch (CompileException ex){
            throw ex;
        } catch (RuntimeException ex){
            throw ex;
        }catch (Exception ex) {
            throw new CompileException(ex);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }

    }

}

/**
 * Wraps a classloader to delegate to parent for getResourceAsStream
 * @author Christian
 */
class ClassResourceClassLoader extends ClassLoader {

    public ClassResourceClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        ClassLoader parent = this.getParent();
        while(parent != null) {
            InputStream ret = parent.getResourceAsStream(name);
            if(ret != null)
                return ret;
            else
                parent = parent.getParent();
        }
        return null;
    }



}

class ParentLimitedSourceReader implements SourceReader {

    private final SourceReader delegate;
    private final ClassResourceClassLoader loader;

    public ParentLimitedSourceReader(SourceReader delegate, ClassResourceClassLoader loader) {
        this.delegate = delegate;
        this.loader = loader;
    }

    public char[] getSource(String[] nameA, boolean fullPath) throws Exception {
        //try to read the source from the classpath
        String name = nameA[0];
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        //only read from the delegate if there is no accordingly named class in parent
        int cut = name.lastIndexOf('.');
        if (cut > 0) {
            String clName = name.substring(0, cut);
            clName = clName.replace('/', '.');
            try {
                loader.loadClass(clName);
                throw new IOException("No classpath-resource found with name: "+name);
            } catch (IOException ex) {
                throw ex;
            } catch (ClassNotFoundException ex) {
                //exception so no class and we can give to the delegate
            }
        }
        return delegate.getSource(nameA,fullPath);
    }

}

class PathSourceReader extends TetiSourceReader {

    private final ClassLoader loader;

    public PathSourceReader(ClassLoader loader) {
        this.loader = loader;
    }

    protected char[] getSourceImpl(String[] nameA,boolean fullPath) throws Exception {
        //try to read the source from the classpath
        String name = nameA[0];
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        InputStream stream = loader.getResourceAsStream(name);
        if (stream == null) {
            throw new FileNotFoundException(name);
        }

        char[] buf = new char[0x8000];
        int l = 0;
        try {
            Reader reader = new java.io.InputStreamReader(stream, "UTF-8");
            for (int n; (n = reader.read(buf, l, buf.length - l)) >= 0;) {
                if (buf.length - (l += n) < 0x1000) {
                    char[] tmp = new char[buf.length << 1];
                    System.arraycopy(buf, 0, tmp, 0, l);
                    buf = tmp;
                }
            }
        } catch (IOException exi) {
            throw new IOException(name + ": " + exi.getMessage());
        } finally {
            try {
                stream.close();
            } catch (Exception ex) {
            }
            
        }
        char[] r = new char[l];
        System.arraycopy(buf, 0, r, 0, l);
        return r;
    }
}



