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

import java.io.IOException;
import yeti.lang.compiler.SourceReader;

/**
 *
 * @author Christian
 */
class DelegateSourceReader implements SourceReader {

    private final SourceReader first;
    private final SourceReader second;
    public DelegateSourceReader(SourceReader first, SourceReader second) {
        this.first = first;
        this.second = second;
    }

    public char[] getSource(String[] nameA, boolean fullPath) throws Exception {
        if (first == null) {
            if (second == null)
                throw new IOException("No source for "+nameA[0]);
            else
                return second.getSource(nameA, fullPath);
        } else {
            try {
                return first.getSource(nameA, fullPath);
            } catch (IOException ex) {
                if(second == null)
                    throw ex;
                else
                   return second.getSource(nameA, fullPath);
            }
        }
    }
}
