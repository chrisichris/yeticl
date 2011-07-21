/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.yeticl;

/**
 *
 * @author Christian
 */
public class TetiParser {

    static public String parseTeti(String name,String text) {
        char[] chars = text.toCharArray();
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
        return stb.toString().toString();
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
