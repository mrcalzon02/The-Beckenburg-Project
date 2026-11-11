package org.beckenburg;

import java.util.Arrays;
import java.util.PriorityQueue;

/** Host-side acoustic propagation through authored thresholds and stair openings. */
public final class LanAcoustics {
    private LanAcoustics() { }

    /** Ghost-hearing route: extraction occupants never become ghost AI targets. */
    public static int edgeCost(HouseMap map, int from, int to) {
        if(from==HouseMap.EXTRACTION_ROOM||to==HouseMap.EXTRACTION_ROOM)return Integer.MAX_VALUE;
        return physicalEdgeCost(map,from,to);
    }
    /** Real propagation includes the van for speech, machinery and other mundane sounds.
     * Doors attenuate in their actual state; a stair opening never acts like a solid deck. */
    public static int physicalEdgeCost(HouseMap map,int from,int to){
        if(!valid(map,from)||!valid(map,to))return Integer.MAX_VALUE;
        int best=Integer.MAX_VALUE;
        for(LevelDefinition.Portal portal:map.level().portals()){
            if(!portal.joins(from,to))continue;
            int loss=Math.max(1,(int)Math.ceil(portal.loss()));
            if(portal.door()&&!map.doorOpen(portal.id()))loss+=2;
            if(portal.kind().equals("window")&&!map.windowOpen(portal.id()))loss+=2;
            loss+=Math.max(absorption(map.level().room(from).acoustic()),
                absorption(map.level().room(to).acoustic()));
            if(map.outside(from)!=map.outside(to))loss++;
            best=Math.min(best,loss);
        }
        return best;
    }
    private static int absorption(String material){
        if(material==null)return 0;
        String name=material.toLowerCase(java.util.Locale.ROOT);
        // Masonry rooms retain a longer audible tail; upholstered spaces absorb a threshold.
        return name.contains("carpet")||name.contains("soft")||name.contains("upholster")?1:0;
    }
    private static boolean valid(HouseMap map,int room){
        return map!=null&&room>=0&&room<map.roomCount();
    }
    /** Lowest-cost route for ghost hearing; excludes the safe expedition van. */
    public static int cost(HouseMap map,int from,int to){return route(map,from,to,false);}
    public static int physicalCost(HouseMap map,int from,int to){return route(map,from,to,true);}

    /**
     * Real source/listener location adds any bounded architectural thresholds
     * inside the existing 15 gameplay zones. The existing zone graph and wire
     * protocol remain stable; only a live authored door's physical state can
     * alter this extra attenuation. Invalid endpoints never become audible.
     */
    public static int costAt(HouseMap map,int from,LevelDefinition.Vec listener,
                             int to,LevelDefinition.Vec source){
        return positionCost(map,from,listener,to,source,false);
    }
    public static int physicalCostAt(HouseMap map,int from,LevelDefinition.Vec listener,
                                     int to,LevelDefinition.Vec source){
        return positionCost(map,from,listener,to,source,true);
    }
    private static int positionCost(HouseMap map,int from,LevelDefinition.Vec listener,
                                    int to,LevelDefinition.Vec source,boolean includeVan){
        int graph=includeVan?physicalCost(map,from,to):cost(map,from,to);
        if(graph==Integer.MAX_VALUE||listener==null||source==null||
           !Double.isFinite(listener.x()+listener.y()+listener.z()+
                            source.x()+source.y()+source.z())||
           map.roomAt(listener.x(),listener.y(),listener.z())!=from||
           map.roomAt(source.x(),source.y(),source.z())!=to)
            return Integer.MAX_VALUE;
        int extra=map.architecturalSoundLoss(source.x(),source.y(),source.z(),
            listener.x(),listener.y(),listener.z());
        return graph>Integer.MAX_VALUE-extra?Integer.MAX_VALUE:graph+extra;
    }

    /** Weather, host ghost AI and human-ghost hearing use the same real door. */
    public static boolean audibleAt(HouseMap map,int listenerRoom,
                                    LevelDefinition.Vec listener,int sourceRoom,
                                    LevelDefinition.Vec source,LanGhostPursuit.Sound sound,
                                    MissionWeather.State weather){
        if(sound==null||weather==null||!valid(map,listenerRoom)||!valid(map,sourceRoom))
            return false;
        int masking=weather.noiseMask()/4;
        if(map.outside(listenerRoom)||map.outside(sourceRoom))masking*=2;
        return costAt(map,listenerRoom,listener,sourceRoom,source)
            <=Math.max(0,sound.budget-masking);
    }
    private static int route(HouseMap map,int from,int to,boolean includeVan){
        if(!valid(map,from)||!valid(map,to)||!includeVan&&
                (from==HouseMap.EXTRACTION_ROOM||to==HouseMap.EXTRACTION_ROOM))return Integer.MAX_VALUE;
        int[] best = new int[map.roomCount()];
        Arrays.fill(best, Integer.MAX_VALUE);
        best[from] = 0;
        PriorityQueue<int[]> queue = new PriorityQueue<>((a,b) -> Integer.compare(a[1], b[1]));
        queue.add(new int[]{from, 0});
        while (!queue.isEmpty()) {
            int[] state = queue.poll();
            if (state[1] != best[state[0]]) continue;
            if (state[0] == to) return state[1];
            for (LevelDefinition.Portal portal:map.level().portalsFrom(state[0])) {
                int next=portal.a()==state[0]?portal.b():portal.a();
                int edge = includeVan?physicalEdgeCost(map,state[0],next):edgeCost(map,state[0],next);
                if (edge == Integer.MAX_VALUE || state[1] > Integer.MAX_VALUE-edge) continue;
                int value = state[1] + edge;
                if (value < best[next]) { best[next] = value; queue.add(new int[]{next,value}); }
            }
        }
        return Integer.MAX_VALUE;
    }
    /** Outdoor storms mask footsteps, speech and equipment, including ghost AI hearing. */
    public static boolean audible(HouseMap map,int from,int to,LanGhostPursuit.Sound sound,
            MissionWeather.State weather){
        if(sound==null||weather==null||!valid(map,from)||!valid(map,to))return false;
        int masking=weather.noiseMask()/4;
        if(map.outside(from)||map.outside(to))masking*=2;
        return cost(map,from,to)<=Math.max(0,sound.budget-masking);
    }
    public static boolean audible(HouseMap map, int from, int to, LanGhostPursuit.Sound sound) {
        return sound != null && cost(map, from, to) <= sound.budget;
    }
}
