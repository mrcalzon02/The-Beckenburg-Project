package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** A standalone prop edit must not detach authored controls, pipes, bedding or story objects. */
final class HouseAuthoringAttachments {
    private HouseAuthoringAttachments(){}
    static boolean required(LevelDefinition source,Prop expected){
        if(!source.id().equals("briar-rebuild"))return false;
        return Set.of("level_trace_generator","level_boiler_modern_worn","level_radiator_worn","level_kitchen_counter","level_laundry_washer","level_bathtub","toilet_mp_1","level_bath_basin").contains(expected.mesh())
            ||source.props().stream().anyMatch(child->child.room()==expected.room()&&(child.support().equals(expected.mesh())
                ||(expected.mesh().equals("bed_4")||expected.mesh().equals("bed_5"))&&child.support().equals("source-bed-origin:"+expected.room())))
            ||source.sockets().stream().anyMatch(socket->socket.room()==expected.room()&&socket.type().equals("authored-clutter")&&socket.style().contains("|"+expected.mesh()+"|"));
    }
    static void validate(LevelDefinition source,List<Prop> props){
        for(var expected:source.props())if(required(source,expected)&&!props.contains(expected))
            throw new IllegalArgumentException("House attachment conflict: "+expected.mesh()+"/room"+expected.room()+": edit the complete service/story assembly with its authored attachments");
    }
    static void change(LevelDefinition source,Prop before,Prop after,String id){
        if(Objects.equals(before,after))return;
        for(var expected:source.props())if(expected.room()==before.room()&&expected.mesh().equals(before.mesh())&&required(source,expected))
            throw new IllegalArgumentException("House attachment conflict: "+id+": "+before.mesh()+" has authored controls, services or supported objects; standalone edit refused");
    }
}
