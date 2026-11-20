package org.beckenburg;

import java.util.Objects;
import java.util.prefs.Preferences;

/** Local, persisted pointer tuning. No network, ghost or movement simulation state. */
public final class MousePreferences {
    private MousePreferences(){}
    public record Settings(int sensitivityPercent,boolean invertY){
        public Settings {
            if(sensitivityPercent<25||sensitivityPercent>200)
                throw new IllegalArgumentException("Mouse sensitivity must be 25..200%");
        }
    }
    private static final Preferences PREF=Preferences.userNodeForPackage(MousePreferences.class);
    private static volatile Settings active=read();
    private static Settings read(){
        int sensitivity=Math.max(25,Math.min(200,PREF.getInt("mouseSensitivityPercent",100)));
        return new Settings(sensitivity,PREF.getBoolean("invertMouseY",false));
    }
    public static Settings current(){return active;}
    public static void save(Settings settings){
        Settings validated=Objects.requireNonNull(settings);
        PREF.putInt("mouseSensitivityPercent",validated.sensitivityPercent());
        PREF.putBoolean("invertMouseY",validated.invertY());
        active=validated;
    }
}
