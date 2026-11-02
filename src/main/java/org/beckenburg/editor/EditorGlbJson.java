package org.beckenburg.editor;

import java.util.*;

/** Deterministic JSON emission for compiler-owned glTF metadata; no code evaluation. */
final class EditorGlbJson {
    private EditorGlbJson(){}
    static String write(Object value){var out=new StringBuilder();append(out,value);return out.toString();}
    private static void append(StringBuilder out,Object value){
        if(value==null){out.append("null");return;}if(value instanceof String s){quote(out,s);return;}if(value instanceof Boolean){out.append(value);return;}
        if(value instanceof Number n){if(!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException("Nonfinite glTF JSON number");out.append(n);return;}
        if(value instanceof Map<?,?> m){out.append('{');boolean comma=false;var keys=new ArrayList<String>();for(Object k:m.keySet())keys.add((String)k);Collections.sort(keys);for(String key:keys){if(comma)out.append(',');comma=true;quote(out,key);out.append(':');append(out,m.get(key));}out.append('}');return;}
        if(value instanceof List<?> a){out.append('[');for(int i=0;i<a.size();i++){if(i>0)out.append(',');append(out,a.get(i));}out.append(']');return;}
        throw new IllegalArgumentException("Unsupported glTF JSON value");
    }
    private static void quote(StringBuilder out,String value){out.append('"');for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");default->{if(c<32)out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}}}out.append('"');}
}
