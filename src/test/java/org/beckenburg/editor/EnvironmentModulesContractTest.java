package org.beckenburg.editor;

import java.util.List;

public final class EnvironmentModulesContractTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static final class Probe implements EnvironmentModule {
        final String id;
        boolean ready;
        int attached,detached;
        Probe(String id,boolean ready){this.id=id;this.ready=ready;}
        public String id(){return id;}
        public String displayName(){return id;}
        public List<Capability> capabilities(){return List.of(Capability.LIVE_PREVIEW);}
        public boolean ready(){return ready;}
        public void attach(RuntimeScene scene){attached++;}
        public void detach(){detached++;}
    }
    public static void main(String[] args){
        EnvironmentModules modules=new EnvironmentModules();
        Probe water=new Probe("water",true),weather=new Probe("weather",true),
              lighting=new Probe("lighting",false);
        modules.register(water);modules.register(weather);modules.register(lighting);
        check(modules.ready().size()==2,"Unfinished lighting remains optional");
        InMemoryRuntimeScene scene=new InMemoryRuntimeScene("test");
        modules.attach(scene);
        check(water.attached==1&&weather.attached==1&&lighting.attached==0,
            "Attach only ready modules");
        modules.detach();
        check(water.detached==1&&weather.detached==1&&lighting.detached==0,
            "Detach only attached modules");
        boolean duplicate=false;
        try{modules.register(new Probe("weather",true));}
        catch(IllegalArgumentException expected){duplicate=true;}
        check(duplicate,"Reject duplicate registrations");
        System.out.println("PASS optional environment module integration");
    }
}
