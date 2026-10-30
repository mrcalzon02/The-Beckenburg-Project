package org.beckenburg.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Independent fake game: no Beckenburg game-engine or Swing dependency. */
public final class RuntimeSceneContractTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static final class OtherGame implements RuntimeScene {
        private List<Node> nodes=new ArrayList<>();
        public String sceneId(){return "unrelated-test-world";}
        public List<Node> nodes(){return List.copyOf(nodes);}
        public void replace(Node node){
            nodes.removeIf(old->old.id().equals(node.id()));
            nodes.add(node);
        }
        public void remove(String id){nodes.removeIf(node->node.id().equals(id));}
        public Snapshot snapshot(){return new State(List.copyOf(nodes));}
        public void restore(Snapshot snapshot){nodes=new ArrayList<>(((State)snapshot).nodes);}
        private record State(List<Node> nodes) implements Snapshot {}
    }
    public static void main(String[] args){
        OtherGame game=new OtherGame();
        RuntimeScene.Component light=new RuntimeScene.Component("light",Map.of("range","8.5"));
        RuntimeScene.Node room=new RuntimeScene.Node("room","Room",null,"geometry",
            RuntimeScene.Transform.identity(),List.of());
        RuntimeScene.Node lamp=new RuntimeScene.Node("lamp","Lamp","room","lights",
            new RuntimeScene.Transform(2,3,5,0,45,0,1,1,1),List.of(light));
        game.replace(room);
        game.replace(lamp);
        check(game.nodes().size()==2,"Scene records nodes");
        check(game.find("lamp").parentId().equals("room"),"Parent ID is stable");
        RuntimeScene.Snapshot before=game.snapshot();
        game.remove("lamp");
        check(game.find("lamp")==null,"Removal works");
        game.restore(before);
        check(game.find("lamp").components().get(0).properties().get("range").equals("8.5"),
            "Snapshot restores typed component metadata");
        boolean invalid=false;
        try{new RuntimeScene.Transform(0,0,0,0,0,0,0,1,1);}
        catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"Reject zero scale");
        invalid=false;
        try{new RuntimeScene.Node("same","Bad","same","props",
            RuntimeScene.Transform.identity(),List.of());}
        catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"Reject direct parenting cycle");
        System.out.println("PASS portable runtime scene identity, components, hierarchy and snapshot");
    }
}
