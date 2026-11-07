package org.beckenburg;
import java.nio.file.*;
public class AnnexSpawnControl{public static void main(String[] args)throws Exception{System.setProperty("user.home",Files.createTempDirectory("annex-spawn-control-").toString());try{CastleLevelEdits.current().setSpawn(-14,18.4,Math.PI);throw new AssertionError("Expected current native spawn restriction");}catch(IllegalArgumentException expected){System.out.println("REPRODUCED current signed annex spawn rejection: "+expected.getMessage());}}}
