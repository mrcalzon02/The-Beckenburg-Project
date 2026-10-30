package org.beckenburg;

import java.util.Objects;

/** Semantic audio for completed field actions, shared by solo and LAN handlers.
 * Call only after authoritative success; a rejected action must never play an
 * equipment, purchase, ward or ritual success cue. No hidden ghost state enters.
 */
public final class AudioActionRouting {
    private AudioActionRouting(){}

    public static LanSound.Cue interaction(InteractionFocus.Kind kind,boolean medicine){
        Objects.requireNonNull(kind);
        if(medicine && kind==InteractionFocus.Kind.RACK)return LanSound.Cue.POTION;
        return switch(kind){
            case CLUTTER -> LanSound.Cue.PICKUP;
            case RACK -> LanSound.Cue.SELECT;
            case FIXTURE,SWITCH -> LanSound.Cue.SWITCH;
            case INTERFACE -> LanSound.Cue.UI_CLICK;
            case BREAKER,GENERATOR -> LanSound.Cue.BREAKER;
            case DOOR,WINDOW -> LanSound.Cue.DOOR;
            case VAN_DOOR -> LanSound.Cue.VAN_DOOR;
        };
    }

    /** A photograph still clicks on an empty-room shot (film was consumed).
     * A false evidence hit is not a rejected camera action. Other rejected uses
     * produce only failure feedback, never an invented successful scan.
     */
    public static LanSound.Cue soloDevice(EquipmentLoadout.Item item,
            boolean accepted,int usesBefore,int usesAfter,boolean uvOn){
        Objects.requireNonNull(item);
        if(!accepted && !(item==EquipmentLoadout.Item.PHOTO_CAMERA
                &&usesBefore>usesAfter))
            return LanSound.Cue.UI_REJECT;
        return switch(item){
            case EMF,THERMOMETER,SPIRIT_BOX -> LanSound.Cue.SCANNER;
            case UV_LAMP -> uvOn?LanSound.Cue.DEVICE_ON:LanSound.Cue.DEVICE_OFF;
            case PHOTO_CAMERA -> LanSound.Cue.CAMERA;
            case SALT -> LanSound.Cue.DROP;
        };
    }

    public static LanSound.Cue counter(HunterCounters.Kind kind){
        Objects.requireNonNull(kind);
        return kind==HunterCounters.Kind.SALT?LanSound.Cue.DROP:LanSound.Cue.WARD;
    }
    public static LanSound.Cue portableLight(boolean placed){
        return placed?LanSound.Cue.DROP:LanSound.Cue.PICKUP;
    }
}
