"""Shared native-source metadata; no geometry or textures are changed."""
def apply(scene,definition):
 name=definition['id'];scene.unit_settings.system='METRIC';scene.unit_settings.scale_length=1
 scene['coordinate_contract']='Metres; Blender Z up, front -Y; runtime Y up, front +Z'
 scene['license_scope']='Original standalone library files: CC0-1.0. Mixed-source game assemblies retain their separate asset licenses.'
 if name=='castle-surroundings':
  scene['source_of_truth']='CastleSurroundings.java / CastleHubArchitecturalPlan.java';scene['assembly_role']='Surroundings only; castle architecture remains in the game hub'
 elif name=='arrival-road':
  scene['source_of_truth']='VanArrivalTile.java / VanArrivalStreet.java / VanStaging.java';scene['assembly_role']='Canonical reusable van and arrival road module; apply destination dock once'
 elif name.startswith('enclosure-'):
  scene['source_of_truth']='LevelEnclosure.java / CastleSurroundings.java';scene['assembly_role']='Reusable surroundings template; investigation building is not included'
 else:
  scene['source_of_truth']='LevelPlans.java / LevelDefinition.java / FixtureLighting.java';scene['assembly_role']='Complete authored investigation level with runtime-matching geometry and functional metadata'
