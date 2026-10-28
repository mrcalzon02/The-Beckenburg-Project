package org.beckenburg;

import hbnet.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Real headless listener lifecycle with an incomplete handshake. Not TLS gameplay acceptance. */
public final class DedicatedLifecycleTest {
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("beckenburg-headless-");
        try{
            Hbnet.Server transport=Setup.initialize(root,null);int port=0;
            for(int i=0;i<3;i++){
                try(LanHost host=LanHost.dedicated("127.0.0.1",port,GhostIdentity.Type.SPIRIT,CustomContract.standard(2),HouseMap.Site.HOUSE,transport)){
                    port=host.port();if(host.voicePort()!=0||host.connected()!=0)throw new AssertionError("Headless host consumes no player or voice slot");
                    try{host.startDedicated();throw new AssertionError("Must require configured ready party");}catch(IllegalArgumentException expected){}
                    // Pending unauthenticated socket must not keep shutdown or port rebinding alive.
                    try(Socket stalled=new Socket("127.0.0.1",port)){if(stalled.isClosed())throw new AssertionError("listener missing");}
                }
            }
            System.out.println("PASS DedicatedLifecycleTest: three no-GUI starts/stops, same port rebind, zero phantom seats/UDP, ready validation, pending socket cutoff");
        }finally{try(var files=Files.walk(root)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
