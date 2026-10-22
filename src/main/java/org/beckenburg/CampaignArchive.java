package org.beckenburg;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.nio.charset.StandardCharsets;

/** Last 32 settled campaign cases, authored only from real resolved rounds. */
public final class CampaignArchive {
    public static final int LIMIT=32;
    private CampaignArchive(){}
    public record CaseFile(String id,SiteCatalog.Location site,InvestigationSession.Role discipline,
            InvestigationSession.Outcome outcome,int credits,int xp,int measuredTools,
            int elapsedTicks,int startLevel,int finalLevel,List<String> journal){
        public CaseFile{
            if(id==null||!id.matches("[0-9a-f\\-]{36}")||site==null||!site.playable()
                ||discipline==null||outcome==null||outcome==InvestigationSession.Outcome.ACTIVE
                ||credits<0||credits>100000||xp<0||xp>10000||measuredTools<0||measuredTools>3
                ||elapsedTicks<0||startLevel<1||startLevel>HunterProgression.MAX_LEVEL
                ||finalLevel<startLevel||finalLevel>HunterProgression.MAX_LEVEL
                ||journal==null||journal.size()>12)
                    throw new IllegalArgumentException("invalid case archive");
            journal=List.copyOf(journal);
            if(journal.stream().anyMatch(s->s==null||s.length()>240
                    ||s.indexOf('\n')>=0||s.indexOf('\r')>=0))
                throw new IllegalArgumentException("invalid case journal");
        }
        public String label(){
            return site.label+"  /  "+outcome+"  /  £"+credits+"  /  XP +"+xp;
        }
        public String fullReport(){
            return "CASE "+id+"\nLOCATION "+site.label+"\nHUNTER "+discipline+
                "  LV "+startLevel+" -> "+finalLevel+
                "\nOUTCOME "+outcome+"\nCOMPENSATION £"+credits+
                "  /  XP +"+xp+"\nTOOLS SAMPLED "+measuredTools+
                "  /  TIME "+(elapsedTicks/600)+"m "+(elapsedTicks/10%60)+"s"+
                "\n\nFIELD JOURNAL\n"+String.join("\n",journal);
        }
    }
    public static CaseFile from(InvestigationSession session,SiteCatalog.Location site,
            int xp,int before,int after){
        if(session.outcome()==InvestigationSession.Outcome.ACTIVE)
            throw new IllegalArgumentException("active case");
        var source=session.journal();
        var journal=new ArrayList<String>();
        for(int i=Math.max(0,source.size()-12);i<source.size();i++){
            String line=source.get(i).replace("\n"," ").replace("\r"," ");
            journal.add(line.substring(0,Math.min(240,line.length())));
        }
        return new CaseFile(session.missionId(),site,session.role(),session.outcome(),
            session.payout(),xp,session.evidenceCount(),session.ticks(),before,after,journal);
    }
    public static void save(Properties p,List<CaseFile> history){
        if(history.size()>LIMIT)throw new IllegalArgumentException("archive overflow");
        p.setProperty("archive.count",Integer.toString(history.size()));
        for(int i=0;i<history.size();i++){
            CaseFile c=history.get(i);String prefix="archive."+i+".";
            p.setProperty(prefix+"id",c.id());
            p.setProperty(prefix+"site",c.site().name());
            p.setProperty(prefix+"role",c.discipline().name());
            p.setProperty(prefix+"outcome",c.outcome().name());
            p.setProperty(prefix+"credits",Integer.toString(c.credits()));
            p.setProperty(prefix+"xp",Integer.toString(c.xp()));
            p.setProperty(prefix+"tools",Integer.toString(c.measuredTools()));
            p.setProperty(prefix+"ticks",Integer.toString(c.elapsedTicks()));
            p.setProperty(prefix+"start",Integer.toString(c.startLevel()));
            p.setProperty(prefix+"end",Integer.toString(c.finalLevel()));
            var combined=String.join("\n",c.journal());
            p.setProperty(prefix+"journal",Base64.getEncoder().encodeToString(
                combined.getBytes(StandardCharsets.UTF_8)));
        }
    }
    public static List<CaseFile> load(Properties p){
        int count=Integer.parseInt(p.getProperty("archive.count","0"));
        if(count<0||count>LIMIT)throw new IllegalArgumentException("invalid archive count");
        ArrayList<CaseFile> result=new ArrayList<>();
        java.util.HashSet<String> ids=new java.util.HashSet<>();
        for(int i=0;i<count;i++){
            String key="archive."+i+".";
            String raw=p.getProperty(key+"journal","");
            if(raw.length()>16000)throw new IllegalArgumentException("oversized archived journal");
            String journal=new String(Base64.getDecoder().decode(raw),StandardCharsets.UTF_8);
            List<String> entries=journal.isEmpty()?List.of():List.of(journal.split("\n",-1));
            CaseFile c=new CaseFile(p.getProperty(key+"id"),
                SiteCatalog.Location.valueOf(p.getProperty(key+"site")),
                InvestigationSession.Role.valueOf(p.getProperty(key+"role")),
                InvestigationSession.Outcome.valueOf(p.getProperty(key+"outcome")),
                Integer.parseInt(p.getProperty(key+"credits")),
                Integer.parseInt(p.getProperty(key+"xp")),
                Integer.parseInt(p.getProperty(key+"tools")),
                Integer.parseInt(p.getProperty(key+"ticks")),
                Integer.parseInt(p.getProperty(key+"start")),
                Integer.parseInt(p.getProperty(key+"end")),entries);
            if(!ids.add(c.id()))throw new IllegalArgumentException("duplicate archive id");
            result.add(c);
        }
        return List.copyOf(result);
    }
}
