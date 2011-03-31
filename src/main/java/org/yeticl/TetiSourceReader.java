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
abstract class TetiSourceReader implements SourceReader {

    public static final String TETI_EXT = ".teti";

    public char[] getSource(String[] name_, boolean fullPath) throws Exception {
        //return getSourceImpl(name_,fullPath);
        String name = name_[0];
        try {
            if(name.endsWith(TETI_EXT)) {
                name_[0] = name.substring(0,name.length() - 5) + ".yeti";
                return tryLoadTeti(name,fullPath,null);
            }else {
                char[] r = getSourceImpl(name_, fullPath);
                return r;
            }
        }catch (IOException ex){

            if(name.endsWith(".yeti")) {
                return tryLoadTeti(name,fullPath,ex);
            }else
                throw ex;
        }
    }

    private char[] tryLoadTeti(String name,boolean fullPath, Exception ex) throws Exception {
        String[] newName = new String[] {name.substring(0,name.length() - 5) + TETI_EXT};
        try{
            char[] chars = getSourceImpl(newName, fullPath);
            char[] ret = parseHeti(newName[0],chars);
            return ret;
        }catch (IOException ex2) {
            if(ex == null)
                throw ex2;
            throw ex;
        }

    }

    abstract protected char[] getSourceImpl(String[] name_, boolean fullPath) throws Exception;

    static public char[] parseHeti(String name,char[] chars) {
        StringBuilder stb = new StringBuilder((int)(chars.length + 1024));
        
        //rawCode (text code)*
        int i = 0;
        //a potetnial skip initial code marker
        if(chars.length > 1 && chars[0] == '<' && chars[1] == '%') {
            i = 2;
        }
        //read the inital code
        i = readRawCode(stb,chars,i);
        while (i < chars.length) {
            i = readString(stb, chars, i);
            if(i < chars.length)
                i = readCode(stb, chars, i);
            else{
                //a string was the end of the file so we finish with a done
                stb.append(" done;");
            }
        }
            
        
        if(!"false".equals(System.getProperty("org.yeticl.print-teti","false"))) {
            System.out.println("TETI:  "+name+ "modified code:");
            System.out.println(stb.toString());
        }
        return stb.toString().toCharArray();
    }

    static private int readCode(StringBuilder stb, char[] chars, int i) {
        
        if(chars[i] == '=') {
            //expression
            i = i +1;
            i = readExpressionCode('e',stb, chars, i);
        }else if(chars.length > (i + 1) && chars[i+1] == '=' && Character.isLetter(chars[i])) {
            i = i + 2;
            i = readExpressionCode(chars[i],stb, chars, i);
        }else{
            i = readRawCode(stb,chars,i);
        }
        return i;
    }

    static private int readExpressionCode(char writerLetter,StringBuilder stb, char[] chars, int i) {
        stb.append("writers.").append(writerLetter).append("(string (");
        i = readRawCode(stb,chars,i);
        stb.append("));");
        return i;
    }


    static private int readRawCode(StringBuilder stb, char[] chars, int i) {
        final int length = chars.length;
        while (i < length){
            char c = chars[i];
            if(c == '%' && (i +1) < length && chars[i+1] == '>') {
                //we have ended the code
                //skip %>
                i = i + 2;
                return i;
            }else{
                i = i + 1;
                if(c == '%' && (i+1) < length && chars[i] == '%' && chars[i+1] == '>'){
                    //escaped %%> make to %> in code
                    stb.append("%>");
                    i = i + 2; //skip the %%>
                }else{
                    stb.append(c);
                }
            }
        }
        return i;
    }


    static private int readString(StringBuilder stb, char[] chars, int i) {
        stb.append("writers.u ('");
        i = readRawString(stb,chars,i);
        stb.append("');");
        return i;
    }

    static private int readRawString(StringBuilder stb, char[] chars, int i) {

        final int length = chars.length;
        while (i < length){
            char c = chars[i];
            if(c == '<' && (i +1) < length && chars[i+1] == '%') {
                if((i+2) < length && chars[i+2] == '%') {
                    stb.append("<%");
                    i = i + 3;
                } else {
                    //string finished because code starts
                    i = i + 2;
                    return i;
                }
            }else{
                i = i +1;
                if(c == '\'') {
                    stb.append("' ^ \"'\" ^ '");
                }else{
                    stb.append(c);
                }
            }
        }
        return i;
    }

}
