package org.beckenburg.editor;

import java.util.*;
import java.io.IOException;

/** Deterministic JSON emission for compiler-owned glTF metadata; no code evaluation. */
final class EditorGlbJson {
    private EditorGlbJson(){}
    static Object read(String text)throws java.io.IOException{return new Json(text).read();}
    static String write(Object value){var out=new StringBuilder();append(out,value);return out.toString();}
    private static void append(StringBuilder out,Object value){
        if(value==null){out.append("null");return;}if(value instanceof String s){quote(out,s);return;}if(value instanceof Boolean){out.append(value);return;}
        if(value instanceof java.math.BigDecimal decimal){out.append(decimal.toString());return;}
        if(value instanceof Number n){if(!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException("Nonfinite glTF JSON number");out.append(n);return;}
        if(value instanceof Map<?,?> m){out.append('{');boolean comma=false;var keys=new ArrayList<String>();for(Object k:m.keySet())keys.add((String)k);Collections.sort(keys);for(String key:keys){if(comma)out.append(',');comma=true;quote(out,key);out.append(':');append(out,m.get(key));}out.append('}');return;}
        if(value instanceof List<?> a){out.append('[');for(int i=0;i<a.size();i++){if(i>0)out.append(',');append(out,a.get(i));}out.append(']');return;}
        throw new IllegalArgumentException("Unsupported glTF JSON value");
    }
    private static void quote(StringBuilder out,String value){out.append('"');for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");default->{if(c<32)out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}}}out.append('"');}
    private static final class Json {
        private static final java.util.regex.Pattern NUMBER=java.util.regex.Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");
        final String s;int p,depth,nodes;
        Json(String data){s=data;}
        Object read()throws IOException{Object x=value();ws();if(p!=s.length())fail();return x;}
        void ws(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;}
        void fail()throws IOException{throw new IOException("Malformed glTF JSON offset "+p);}
        Object value()throws IOException{
            if(++nodes>250000||depth>=64)throw new IOException("glTF JSON structural budget exceeded");depth++;try{return nextValue();}finally{depth--;}
        }
        Object nextValue()throws IOException{
            ws();if(p==s.length()) {fail();return null;}
            char c=s.charAt(p++);
            if(c=='{'){
                Map<String,Object> m=new HashMap<>();ws();
                if(p<s.length()&&s.charAt(p)=='}'){p++;return m;}
                while(true){ws();if(p>=s.length()||s.charAt(p)!='"')fail();p++;
                    String key=string();ws();if(p>=s.length()||s.charAt(p++)!=':')fail();
                    if(m.containsKey(key))throw new IOException("Duplicate glTF JSON property");m.put(key,value());ws();if(p>=s.length())fail();
                    char sep=s.charAt(p++);if(sep=='}')break;if(sep!=',')fail();}
                return m;
            }
            if(c=='['){List<Object> a=new ArrayList<>();ws();
                if(p<s.length()&&s.charAt(p)==']'){p++;return a;}
                while(true){a.add(value());ws();if(p>=s.length())fail();
                    char sep=s.charAt(p++);if(sep==']')break;if(sep!=',')fail();}
                return a;
            }
            if(c=='"')return string();
            if(c=='t'&&s.startsWith("rue",p)){p+=3;return true;}
            if(c=='f'&&s.startsWith("alse",p)){p+=4;return false;}
            if(c=='n'&&s.startsWith("ull",p)){p+=3;return null;}
            int start=p-1;
            while(p<s.length()&&"-+.0123456789eE".indexOf(s.charAt(p))>=0)p++;
            if(start==p)fail();
            try{String token=s.substring(start,p);if(token.length()>128||!NUMBER.matcher(token).matches())fail();return new java.math.BigDecimal(token);}catch(NumberFormatException bad){fail();return null;}
        }
        String string()throws IOException{
            StringBuilder b=new StringBuilder();
            while(p<s.length()){char c=s.charAt(p++);boolean escaped=false;
                if(c=='"')return b.toString();
                if(c=='\\'){escaped=true;if(p==s.length())fail();c=s.charAt(p++);
                    if(c=='u'){if(p+4>s.length())fail();try{
                        c=(char)Integer.parseInt(s.substring(p,p+4),16);
                    }catch(NumberFormatException bad){fail();}p+=4;}
                    else c=switch(c){case 'n'->'\n';case 'r'->'\r';case 't'->'\t';
                        case '"'->'"';case '\\'->'\\';case '/'->'/';case 'b'->'\b';case 'f'->'\f';
                        default->throw new IOException("JSON escape");};}
                if(c<32&&!escaped)throw new IOException("Unescaped glTF JSON control character");b.append(c);
            }
            fail();return "";
        }
    }
}
