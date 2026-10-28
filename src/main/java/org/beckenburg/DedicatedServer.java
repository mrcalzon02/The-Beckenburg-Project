package org.beckenburg;

import hbnet.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Headless case operator using the same LanHost. No UI, renderer or media socket.
 * The local console shares the listen-host start validator without consuming a player seat.
 * This is a temporary case, not a campaign-save or host-migration service.
 */
public final class DedicatedServer {
    private DedicatedServer(){}
    public static void main(String[] args)throws Exception{
        if(args.length<1||args.length>5)throw new IllegalArgumentException("Use DedicatedServer network-data [port] [hunters 1-6] [explicit-bind] [site]");
        Path root=Path.of(args[0]);int port=args.length>1?Hbnet.port(args[1]):27717,hunters=args.length>2?Integer.parseInt(args[2]):3;
        if(hunters<1||hunters>6)throw new IllegalArgumentException("Hunters must be 1-6");
        Hbnet.Server transport=Hbnet.Server.load(root);HouseMap.Site site=args.length>4?HouseMap.Site.valueOf(args[4]):HouseMap.Site.HOUSE;
        try(LanHost host=LanHost.dedicated(args.length>3?args[3]:"127.0.0.1",port,GhostIdentity.Type.SPIRIT,CustomContract.standard(hunters),site,transport)){
            Runtime.getRuntime().addShutdownHook(new Thread(host::close));
            System.out.println("Private headless case ready. UDP voice disabled. Console: status, start, lock, unlock, invite, stop.");
            BufferedReader console=new BufferedReader(new InputStreamReader(System.in));String line;
            while((line=console.readLine())!=null){
                try{
                    switch(line){
                        case "status" -> System.out.println("Players "+host.connected()+"; reserved "+host.reservedSeats()+"; active "+host.running());
                        case "start" -> host.startDedicated();
                        case "lock" -> host.setLobbyLocked(true);
                        case "unlock" -> host.setLobbyLocked(false);
                        case "invite" -> {if(args.length<4)throw Hbnet.failure();Path file=root.resolve("invitation.secret");Files.deleteIfExists(file);Files.createFile(file);Hbnet.protect(file);Files.writeString(file,transport.invitation("beckenburg",args[3],host.port(),600));System.out.println("Signed endpoint invitation written privately. It does not contain the room password.");}
                        case "stop" -> {return;}
                        default -> System.out.println("Unknown local command (NET-ADMIN)");
                    }
                }catch(Exception error){System.out.println("Command unavailable (NET-ADMIN)");}
            }
        }
    }
}
