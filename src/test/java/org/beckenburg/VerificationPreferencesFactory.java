package org.beckenburg;
import java.util.*;
import java.util.prefs.*;

/** Render/test processes can use preferences without reading or changing the user's settings. */
public final class VerificationPreferencesFactory implements PreferencesFactory {
    private final Preferences user=new Memory(null,""),system=new Memory(null,"");
    public Preferences userRoot(){return user;}
    public Preferences systemRoot(){return system;}
    private static final class Memory extends AbstractPreferences {
        private final Map<String,String> values=new HashMap<>();
        private final Map<String,Memory> children=new HashMap<>();
        Memory(AbstractPreferences parent,String name){super(parent,name);}
        protected void putSpi(String k,String v){values.put(k,v);}
        protected String getSpi(String k){return values.get(k);}
        protected void removeSpi(String k){values.remove(k);}
        protected void removeNodeSpi(){values.clear();children.clear();}
        protected String[] keysSpi(){return values.keySet().toArray(String[]::new);}
        protected String[] childrenNamesSpi(){return children.keySet().toArray(String[]::new);}
        protected AbstractPreferences childSpi(String n){return children.computeIfAbsent(n,k->new Memory(this,k));}
        protected void syncSpi(){}
        protected void flushSpi(){}
    }
}
