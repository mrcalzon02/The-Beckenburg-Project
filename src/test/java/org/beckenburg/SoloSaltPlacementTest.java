package org.beckenburg;

/** Functional solo salt placement must use the selected physical orientation. */
public final class SoloSaltPlacementTest {
    private static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public static void main(String[] args){
        HouseMap map=new HouseMap();
        var spawn=map.spawn(4);
        var session=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,20261030L);
        var loadout=new EquipmentLoadout();
        loadout.replaceAtRack(new EquipmentLoadout.Item[]{
            EquipmentLoadout.Item.EMF,EquipmentLoadout.Item.THERMOMETER,
            EquipmentLoadout.Item.SPIRIT_BOX,EquipmentLoadout.Item.SALT});
        loadout.select(3);
        double yaw=Math.toRadians(33);
        check(session.placedSalt()==null,"nothing placed before use");
        check(session.useEquipment(loadout,spawn.x(),spawn.y(),spawn.z(),yaw,90),
            "left click placement succeeds on an actual traversable floor");
        var placed=session.placedSalt();
        check(placed!=null,"accepted placement preserved in world model state");
        check(placed.room()==4&&Math.abs(placed.x()-spawn.x())<.001
            &&Math.abs(placed.z()-spawn.z())<.001,"real player position retained");
        check(Math.abs(placed.headingRadians()-Math.toRadians(123))<1e-8,
            "wheel degrees modify the original camera direction");
        check(loadout.usesLeft(EquipmentLoadout.Item.SALT)==2,
            "placing consumes one real pouch");
        check(EquipmentModelCatalog.variant("SALT",EquipmentTiers.Tier.I,"deployed")!=null,
            "the accepted object has an authored deployed mesh");
        System.out.println("SoloSaltPlacementTest PASS");
    }
}
