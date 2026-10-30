package org.beckenburg;

/**
 * Nonblocking map implementation inventory. Reports geometry/room and portal
 * readiness without turning visual/design imperfections into release gates.
 */
public final class AllAuthoredMapsDiagnostic {
    public static void main(String[] args) {
        int ready=0;
        for (SiteCatalog.Location location : SiteCatalog.Location.values()) {
            try {
                if (!location.playable()) {
                    System.out.println("DIAGNOSTIC "+location.name()+": unavailable (geometry builder rejected)");
                    continue;
                }
                HouseMap map=new HouseMap(SiteCatalog.mapFor(location));
                var level=map.level();
                int doors=(int)level.portals().stream().filter(p->p.door()).count();
                int indoor=(int)level.rooms().stream().filter(r->!r.outside()).count();
                int basement=(int)level.rooms().stream().filter(r->r.y()<0).count();
                System.out.println("MAP "+location.name()+" revision="+level.revision()+
                    " indoor="+indoor+" doors="+doors+" stairs="+level.stairs().size()+
                    " basementZones="+basement+" props="+level.props().size());
                if (indoor>0 && doors>0) ready++;
            } catch (RuntimeException failure) {
                System.out.println("DIAGNOSTIC "+location.name()+": "+failure);
            }
        }
        System.out.println("Authored map runtime inventory: "+ready+"/"+SiteCatalog.Location.values().length);
    }
}
