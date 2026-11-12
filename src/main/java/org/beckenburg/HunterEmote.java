package org.beckenburg;
/** Bounded hunter gesture: other hunters see action; ghost hears only location. */
public enum HunterEmote {
    WAVE("waves"),POINT("points"),SHRUG("shrugs"),LAUGH("laughs");
    private final String verb;
    HunterEmote(String verb){this.verb=verb;}
    public String verb(){return verb;}
    public static HunterEmote fromCommand(String typed){
        if(typed==null)return null;
        return switch(typed.strip().toLowerCase(java.util.Locale.ROOT)){
            case "/wave"->WAVE;
            case "/point"->POINT;
            case "/shrug"->SHRUG;
            case "/laugh"->LAUGH;
            default->null;
        };
    }
}
