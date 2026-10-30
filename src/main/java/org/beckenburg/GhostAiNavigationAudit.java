package org.beckenburg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only authoring diagnostic over the EXACT HouseMap/portal and acoustics rules.
 * Reports route reachability; it never invents traversals or modifies door states.
 */
public final class GhostAiNavigationAudit {
    public record Link(int from,int to,int acousticLoss) {}
    public record Finding(String code,int room,String detail) {}
    public record Report(HouseMap.Site site,int origin,int inspectedRooms,
                         List<Link> links,List<Integer> unreachable,
                         List<Finding> findings) {
        public Report {
            links=List.copyOf(links);
            unreachable=List.copyOf(unreachable);
            findings=List.copyOf(findings);
        }
    }
    private GhostAiNavigationAudit(){}
    /** All NPCs run their real room/portal rules. Extraction/van has NO ghost exits. */
    public static Report inspect(HouseMap map,int start){
        Objects.requireNonNull(map);
        int bound=Math.min(map.roomCount(),HouseMap.EXTRACTION_ROOM);
        if(start<0||start>=bound)throw new IllegalArgumentException("Invalid ghost site room");
        List<Link> links=new ArrayList<>();
        List<Finding> findings=new ArrayList<>();
        boolean[] reach=new boolean[bound];
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        queue.add(start);reach[start]=true;
        for(int from=0;from<bound;from++){
            Set<Integer> unique=new HashSet<>();
            for(int next:map.ghostExits(from)){
                if(next<0||next>=bound){
                    findings.add(new Finding("INVALID_EXIT",from,"Ghost exit outside permitted rooms"));
                    continue;
                }
                if(!unique.add(next))
                    findings.add(new Finding("DUPLICATE_EXIT",from,"Repeated portal to "+next));
                if(!map.ghostExits(next).contains(from))
                    findings.add(new Finding("ONE_WAY_EXIT",from,"No reverse portal from "+next));
                int loss=LanAcoustics.cost(map,from,next);
                links.add(new Link(from,next,loss));
                if(loss==Integer.MAX_VALUE)
                    findings.add(new Finding("SOUND_PATH_MISSING",from,
                        "Traversable portal to "+next+" does not propagate ghost hearing"));
            }
        }
        // Reachability always follows the authoritative graph; a closed door may
        // still be a valid route because the ghost must open it to cross.
        while(!queue.isEmpty()){
            int current=queue.removeFirst();
            for(int next:map.ghostExits(current)){
                if(next>=0&&next<bound&&!reach[next]){reach[next]=true;queue.addLast(next);}
            }
        }
        List<Integer> missing=new ArrayList<>();
        for(int i=0;i<bound;i++)if(!reach[i])missing.add(i);
        if(!missing.isEmpty())
            findings.add(new Finding("UNREACHABLE_ROOMS",start,
                missing.size()+" rooms cannot be reached from chosen ghost origin"));
        if(!map.ghostExits(HouseMap.EXTRACTION_ROOM).isEmpty())
            findings.add(new Finding("VAN_BOUNDARY",HouseMap.EXTRACTION_ROOM,
                "Ghost exits cannot lead from the van"));
        return new Report(map.site(),start,bound,links,missing,findings);
    }
    /** Bounded breadth-first route, using actual game ghost exits, not a second nav graph. */
    public static List<Integer> path(HouseMap map,int from,int to){
        Objects.requireNonNull(map);
        int limit=Math.min(map.roomCount(),HouseMap.EXTRACTION_ROOM);
        if(from<0||to<0||from>=limit||to>=limit)
            throw new IllegalArgumentException("Route cannot originate in or enter extraction");
        int[] parent=new int[limit];
        Arrays.fill(parent,-1);
        boolean[] visited=new boolean[limit];
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        queue.add(from);visited[from]=true;
        while(!queue.isEmpty()&&!visited[to]){
            int node=queue.removeFirst();
            for(int next:map.ghostExits(node)){
                if(next>=0&&next<limit&&!visited[next]){
                    visited[next]=true;parent[next]=node;queue.addLast(next);
                }
            }
        }
        if(!visited[to])return List.of();
        ArrayList<Integer> out=new ArrayList<>();
        for(int step=to;step!=-1;step=parent[step])out.add(step);
        java.util.Collections.reverse(out);
        return List.copyOf(out);
    }
}
