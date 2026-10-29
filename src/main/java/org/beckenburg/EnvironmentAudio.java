package org.beckenburg;

import java.util.prefs.Preferences;

/** Persistent environment-only gain, independent of music, effects and voice. */
public final class EnvironmentAudio {
    private static final Preferences SETTINGS=Preferences.userNodeForPackage(EnvironmentAudio.class);
    private static volatile int volume=Math.max(0,Math.min(100,SETTINGS.getInt("environment_volume",70)));
    private EnvironmentAudio(){}
    public static int volume(){return volume;}
    public static void volume(int value){
        if(value<0||value>100)throw new IllegalArgumentException("Environment volume 0-100");
        volume=value;
        SETTINGS.putInt("environment_volume",value);
    }
}
