package org.beckenburg;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

/** Independent layout and floor paging assertions; no renderer or app UI needed. */
public final class DevAssetGalleryLayoutTest {
    private DevAssetGalleryLayoutTest(){}
    public static void main(String[] args){
        if(DevAssetGalleryLayout.SLOTS_PER_WING!=72)
            throw new AssertionError("Unexpected gallery wing capacity");
        var occupied=new HashSet<String>();
        for(int i=0;i<216;i++){
            var a=DevAssetGalleryLayout.address(i);
            if(a.absoluteIndex()!=i)throw new AssertionError("index/address roundtrip: "+i);
            String contact=a.wing()+"/"+a.floor()+"/"+a.bay()+"/"+a.slot();
            if(!occupied.add(contact))throw new AssertionError("duplicate contact: "+contact);
        }
        var items=new ArrayList<Integer>();
        for(int i=0;i<157;i++)items.add(i);
        int[] counts={72,72,13,0};
        for(int wing=0;wing<counts.length;wing++)
            if(DevAssetGalleryLayout.wing(items,wing).size()!=counts[wing])
                throw new AssertionError("Incorrect wing length: "+wing);
        for(int floor=0;floor<3;floor++){
            var first=DevAssetGalleryLayout.bay(DevAssetGalleryLayout.wing(items,0),floor,0);
            if(first.size()!=6||first.get(0)!=floor*24)
                throw new AssertionError("floor start incorrect: "+floor);
        }
        if(DevAssetGalleryLayout.bay(DevAssetGalleryLayout.wing(items,2),0,2).size()!=1)
            throw new AssertionError("Partial last bay incorrect");
        if(!DevAssetGalleryLayout.bay(DevAssetGalleryLayout.wing(items,2),2,0).isEmpty())
            throw new AssertionError("Empty upper floor must be empty");
        var spots=new HashSet<String>();
        for(int floor=0;floor<3;floor++)for(int bay=0;bay<4;bay++)for(int slot=0;slot<6;slot++){
            String pos=DevAssetGalleryLayout.standX(slot)+":"+
                DevAssetGalleryLayout.floorY(floor)+":"+
                DevAssetGalleryLayout.standZ(bay,slot);
            if(!spots.add(pos))throw new AssertionError("overlapping stands");
        }
        for(int floor=0;floor<3;floor++){
            double eye=DevAssetGalleryLayout.eyeY(floor);
            if(DevAssetGalleryLayout.floorAtEye(eye)!=floor)
                throw new AssertionError("Eye is in the wrong floor");
        }
        for(int bay=0;bay<4;bay++){
            if(DevAssetGalleryLayout.bayAtZ(DevAssetGalleryLayout.bayEntranceZ(bay))!=bay)
                throw new AssertionError("Bay entry camera maps incorrectly");
        }
        if(DevAssetGalleryLayout.floorAtEye(-5)!=0||
            DevAssetGalleryLayout.floorAtEye(100)!=2||
            DevAssetGalleryLayout.bayAtZ(-100)!=0||
            DevAssetGalleryLayout.bayAtZ(100)!=3)
            throw new AssertionError("Out-of-room clamping regressed");
        System.out.println("PASS gallery layout: 3 floors, 4 bays each, 6 unique true-position stands per bay; "+
            "partial-wing and camera-floor routing verified");
    }
}
