package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** One combined indoor/outdoor allowance on every location, independent of size. */
public final class HidingBudget {
    private HidingBudget(){}
    public static final int MAXIMUM=3;
    public static int allowance(int density){
        if(density<0||density>100)throw new IllegalArgumentException("hiding density");
        return MAXIMUM*density/100;
    }
    public static List<Socket> candidates(LevelDefinition level){
        // Keep existing authored outdoor refuges meaningful, then use measured
        // cupboards. Do not manufacture hiding places from decorative foliage.
        return level.sockets().stream().filter(s->s.type().equals("outdoor-hide")
            ||s.type().equals("hide")&&s.style().equals("walk-in-cupboard")&&s.room()>=0&&s.room()<12)
            .sorted(Comparator.comparingInt((Socket s)->s.type().equals("outdoor-hide")?-2:s.room()==1?-1:s.room())
                .thenComparing(Socket::id)).limit(MAXIMUM).toList();
    }
    public static boolean available(LevelDefinition level,Socket socket,int density){
        int n=allowance(density);if(socket==null||n==0)return false;
        var pool=candidates(level);return pool.subList(0,Math.min(n,pool.size())).contains(socket);
    }
}
