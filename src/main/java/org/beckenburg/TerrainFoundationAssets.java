package org.beckenburg;
import java.util.Set;
/** Curated terrain exports only. Approval does not unlock their unfinished maps. */
public final class TerrainFoundationAssets {
 private TerrainFoundationAssets(){}
 public static final Set<String> MODELS=Set.of(
  "level_site_academy_terrain",
  "level_site_asylum_terrain",
  "level_site_college_terrain",
  "level_site_mansion_terrain",
  "level_site_prison_terrain",
  "level_site_wld_03_terrain",
  "level_site_wld_04_terrain",
  "level_site_wld_05_terrain",
  "level_site_wld_06_terrain",
  "level_site_wld_07_terrain",
  "level_site_wld_08_terrain",
  "level_site_wld_09_terrain",
  "level_site_wld_10_terrain",
  "level_site_wld_11_terrain",
  "level_site_wld_12_terrain",
  "level_site_wld_13_terrain");
 public static boolean approved(String id){return MODELS.contains(id);}
}
