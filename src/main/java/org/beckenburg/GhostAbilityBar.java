package org.beckenburg;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Persistent numbered slots for both spectral controllers. Only the host computes readiness. */
final class GhostAbilityBar {
    enum Slot {
        WHISPER, DISTURB, MANIFEST, STRIKE, SIGNATURE, FRENZY, CLAW, DREAD;
        int key(){return ordinal()+1;}
        String label(){return name().charAt(0)+name().substring(1).toLowerCase(java.util.Locale.ROOT);}
    }
    enum State {
        READY("Ready"), AIM("Aim"), REQUEST("Request"), COOLDOWN(""), LOW_ENERGY("Low energy"),
        CALM("Need calm"), RAGE("Need frenzy"), WARDED("Warded"), MANIFEST("Manifest first"), WAIT("Wait");
        final String label;
        State(String label){this.label=label;}
    }
    record Entry(Slot slot,int cost,State state,int remaining,boolean used){
        String caption(){return state==State.COOLDOWN?(remaining+9)/10+"s":state.label;}
        boolean available(){return state==State.READY||state==State.AIM||state==State.REQUEST;}
    }
    record Snapshot(int tick,List<Entry> slots){
        Snapshot{slots=List.copyOf(slots);}
        String packet(){
            StringBuilder out=new StringBuilder("GABILITIES|").append(tick);
            for(Entry e:slots)out.append('|').append(e.slot).append(',').append(e.cost)
                .append(',').append(e.state).append(',').append(e.remaining).append(',').append(e.used?1:0);
            return out.toString();
        }
    }
    private GhostAbilityBar(){}

    static Snapshot decode(List<String> fields){
        if(fields.size()<7||fields.size()>10||!fields.get(0).equals("GABILITIES"))
            throw new IllegalArgumentException("invalid ghost loadout");
        int tick=LanProtocol.integer(fields.get(1),0,99999999);
        List<Entry> entries=new ArrayList<>();
        for(int i=2;i<fields.size();i++){
            String[] values=fields.get(i).split(",",-1);
            if(values.length!=5)throw new IllegalArgumentException("invalid slot");
            Slot slot=Slot.valueOf(values[0]);
            if(!entries.isEmpty()&&slot.key()<=entries.get(entries.size()-1).slot().key())
                throw new IllegalArgumentException("unordered ghost loadout");
            entries.add(new Entry(slot,LanProtocol.integer(values[1],1,100),State.valueOf(values[2]),
                LanProtocol.integer(values[3],0,10000),LanProtocol.integer(values[4],0,1)==1));
        }
        return new Snapshot(tick,entries);
    }
    /** Older v2 hosts publish the legal ordinary actions in STATE but no cooldown clocks. */
    static Snapshot legacy(int tick,GhostIdentity.Type type,String legal,GhostFrenzy.Phase phase){
        List<Entry> entries=new ArrayList<>(8);
        for(var action:GhostDomain.Action.values()){
            if(action==GhostDomain.Action.DRIFT)continue;
            State state=State.WAIT;
            if(phase!=GhostFrenzy.Phase.CALM)state=State.CALM;
            else if(legal.contains(action.name()))state=action==GhostDomain.Action.STRIKE?State.AIM:State.READY;
            entries.add(new Entry(Slot.valueOf(action.name()),GhostDomain.cost(type,action),state,0,false));
        }
        var profile=GhostFrenzy.profile(type);
        entries.add(new Entry(Slot.FRENZY,profile.energyCost(),phase==GhostFrenzy.Phase.CALM?State.REQUEST:State.CALM,0,false));
        for(var attack:GhostFrenzy.Attack.values())if(profile.permits(attack)){
            State state=phase==GhostFrenzy.Phase.RAGING?State.AIM:State.RAGE;
            entries.add(new Entry(Slot.valueOf(attack.name()),profile.cost(attack),state,0,false));
        }
        return new Snapshot(tick,entries);
    }
    static Snapshot inspect(GhostDomain ghost,GhostFrenzy frenzy){
        GhostDomain.Observation observation=ghost.observe();
        int tick=observation.tick();
        boolean calm=frenzy==null||frenzy.phase()==GhostFrenzy.Phase.CALM;
        List<Entry> entries=new ArrayList<>(8);
        for(GhostDomain.Action action:GhostDomain.Action.values()){
            if(action==GhostDomain.Action.DRIFT)continue; // Movement keeps its own Space binding.
            int cost=ghost.cost(action);
            int remaining=ghost.cooldownRemaining(action);
            State state;
            if(!calm)state=State.CALM;
            else if(ghost.suppressed())state=State.WARDED;
            else if(remaining>0)state=State.COOLDOWN;
            else if(ghost.energy()<cost)state=State.LOW_ENERGY;
            else if(!observation.legalActions().contains(action)){
                state=action==GhostDomain.Action.STRIKE&&!observation.manifested()?State.MANIFEST:State.WAIT;
            }
            else state=action==GhostDomain.Action.STRIKE?State.AIM:State.READY;
            entries.add(new Entry(Slot.valueOf(action.name()),cost,state,remaining,tick-ghost.lastUsed(action)<15));
        }
        if(frenzy!=null){
            var profile=frenzy.traits();
            int wait=frenzy.startCooldown(tick);
            State state;
            if(wait>0)state=State.COOLDOWN;
            else if(!calm)state=State.CALM;
            else if(ghost.suppressed())state=State.WARDED;
            else if(ghost.energy()<profile.energyCost())state=State.LOW_ENERGY;
            else state=State.REQUEST; // Eligibility of living targets stays private.
            entries.add(new Entry(Slot.FRENZY,profile.energyCost(),state,wait,tick-frenzy.lastStart()<15));
            for(var attack:GhostFrenzy.Attack.values()){
                if(!profile.permits(attack))continue;
                int remaining=frenzy.cooldownRemaining(attack,tick);
                if(!frenzy.raging())state=State.RAGE;
                else if(ghost.suppressed())state=State.WARDED;
                else if(remaining>0)state=State.COOLDOWN;
                else if(ghost.energy()<profile.cost(attack))state=State.LOW_ENERGY;
                else state=State.AIM; // No victim location, presence or identity in the bar.
                entries.add(new Entry(Slot.valueOf(attack.name()),profile.cost(attack),state,
                    remaining,tick-frenzy.lastUsed(attack)<15));
            }
        }
        return new Snapshot(tick,entries);
    }
    static void draw(Graphics2D original,int width,int bottom,Snapshot snapshot){
        if(snapshot==null||width<200)return;
        Graphics2D g=(Graphics2D)original.create();
        try{
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int count=snapshot.slots.size(),gap=3,cell=Math.min(93,(width-16-(count-1)*gap)/count);
            int x=(width-count*cell-(count-1)*gap)/2,y=bottom-53;
            for(Entry e:snapshot.slots){
                g.setColor(new Color(22,17,30,236));g.fillRoundRect(x,y,cell,51,6,6);
                g.setColor(e.used?new Color(247,200,122):e.available()?new Color(176,212,186):new Color(127,113,139));
                g.drawRoundRect(x,y,cell-1,50,6,6);
                g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,10));
                g.drawString(Integer.toString(e.slot.key()),x+5,y+13);
                String cost=e.cost+" E";
                g.drawString(cost,x+cell-5-g.getFontMetrics().stringWidth(cost),y+13);
                g.setColor(e.available()?new Color(246,241,250):new Color(208,200,215));
                centered(g,e.slot.label(),x,y+30,cell,13,Font.BOLD);
                g.setColor(e.used?new Color(247,200,122):e.available()?new Color(178,231,192):new Color(197,181,206));
                centered(g,e.caption(),x,y+44,cell,11,Font.PLAIN);
                x+=cell+gap;
            }
        }finally{g.dispose();}
    }
    private static void centered(Graphics2D g,String text,int x,int baseline,int width,int size,int style){
        Font font=new Font(Font.SANS_SERIF,style,size);
        int unscaled=g.getFontMetrics(font).stringWidth(text);
        if(unscaled>width-4)font=font.deriveFont(java.awt.geom.AffineTransform.getScaleInstance((width-4.0)/unscaled,1));
        g.setFont(font);
        g.drawString(text,x+(width-g.getFontMetrics().stringWidth(text))/2,baseline);
    }
}
