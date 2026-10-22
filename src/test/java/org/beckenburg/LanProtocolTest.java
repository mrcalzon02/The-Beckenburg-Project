package org.beckenburg;

import java.io.*;
import java.util.List;

public final class LanProtocolTest {
    static void check(boolean value,String description){if(!value)throw new AssertionError(description);}
    static void rejects(Runnable action){try{action.run();throw new AssertionError("should reject");}catch(IllegalArgumentException expected){}}
    public static void main(String[] args)throws Exception {
        check(LanProtocol.parts("JOIN|1|HUNTER|Test").equals(List.of("JOIN","1","HUNTER","Test")),"framing");
        check(LanProtocol.integer("14",0,14)==14,"bounded integer");
        rejects(()->LanProtocol.integer("15",0,14));
        rejects(()->LanProtocol.integer("10.5",0,14));
        rejects(()->LanProtocol.sequence("-1"));
        rejects(()->LanProtocol.parts("BAD|".repeat(50)));
        String visibleCamera="CAMGHOST|0|a|VISIBLE|21000|0|19200|180|BASE|shade|86";
        check(LanProtocol.parts(visibleCamera).size()==11,
            "A real visible camera Ghost frame must be valid on the host and receiver");
        rejects(()->LanProtocol.parts(visibleCamera+"|EXTRA"));
        rejects(()->LanProtocol.cleanName("|JOIN|"));
        StringBuilder oversized=new StringBuilder("ABC");oversized.append("a".repeat(550));
        try{LanProtocol.read(new StringReader(oversized+"\n"));throw new AssertionError("oversized socket frame accepted");}
        catch(IOException expected){}
        check(LanProtocol.read(new StringReader("READY\n")).equals("READY"),"reader");
        try{LanProtocol.read(new StringReader("READY"));throw new AssertionError("truncated frame accepted");}
        catch(IOException expected){}
        System.out.println("PASS: bounded LAN line protocol, input validation, error and EOF handling");
    }
}
