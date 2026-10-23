package org.beckenburg;
import java.util.*;
/** Small verification-report adapter; runtime/export record serialization remains unchanged. */
final class DressingReport {
    private DressingReport(){}
    static String json(Map<String,Object> fields)throws Exception{
        var out=new StringJoiner(",","{","}");for(var entry:fields.entrySet())out.add(LevelSceneSource.json(entry.getKey())+":"+LevelSceneSource.json(entry.getValue()));return out.toString();
    }
}
