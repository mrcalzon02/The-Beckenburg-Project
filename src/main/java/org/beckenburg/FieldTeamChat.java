package org.beckenburg;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

/**
 * Compact solo-team chat using the established LAN local/radio chat envelope.
 * Only local messages may be heard without a radio; callers enforce acoustics
 * through LanChat.localVisible before an AI acts on them.
 */
public final class FieldTeamChat {
    private static final int CAPACITY=28;
    private final ArrayDeque<LanChat.Entry> history=new ArrayDeque<>();
    private int latestTick=-1;
    public void post(LanChat.Channel channel,int seat,int tick,String message){
        Objects.requireNonNull(channel);
        Objects.requireNonNull(message);
        if(tick<0||tick<latestTick)throw new IllegalArgumentException("chat order");
        String safe=message.replace("°","").replace("|","/");
        safe=safe.replaceAll("[^\\x20-\\x7E]","");
        if(safe.isBlank())return;
        if(safe.length()>LanChat.MAX_CHARS)safe=safe.substring(0,LanChat.MAX_CHARS);
        LanChat.Entry entry=new LanChat.Entry(channel,seat,tick,safe.strip());
        latestTick=tick;
        if(history.size()==CAPACITY)history.removeFirst();
        history.addLast(entry);
    }
    public List<LanChat.Entry> messages(){return List.copyOf(history);}
    public int latestTick(){return latestTick;}
}
