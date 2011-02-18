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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 *
 * @author Christian
 */
public class FileSourceReader extends TetiSourceReader {

    public final String[] basedirs;

    public FileSourceReader(String[] basedirs) {
        this.basedirs = basedirs;

    }

    protected char[] getSourceImpl(String[] name_, boolean fullPath) throws IOException {
        char[] buf = new char[0x8000];
        int l = 0;
        InputStream stream;
        String name = name_[0];
        if (fullPath || basedirs == null || basedirs.length == 0) {
            stream = new FileInputStream(name);
        } else {
            int i = 0;
            while (true) {
                try {
                    stream = new FileInputStream(new File(basedirs[i], name));
                    break;
                } catch (IOException ex) {
                    if (++i >= basedirs.length) {
                        throw ex;
                    }
                }
            }
        }

        try {
            Reader reader = new java.io.InputStreamReader(stream, "UTF-8");
            for (int n; (n = reader.read(buf, l, buf.length - l)) >= 0;) {
                if (buf.length - (l += n) < 0x1000) {
                    char[] tmp = new char[buf.length << 1];
                    System.arraycopy(buf, 0, tmp, 0, l);
                    buf = tmp;
                }
            }
        } catch (IOException ex) {
            throw new IOException(name + ": " + ex.getMessage());
        } finally {
            stream.close();
        }
        name_[0] = name;
        char[] r = new char[l];
        System.arraycopy(buf, 0, r, 0, l);
        return r;
    }
}
