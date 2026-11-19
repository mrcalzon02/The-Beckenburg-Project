# Briar House entrance and porch

At the front elevation, portal 14 connects the front garden and entrance vestibule. The shared sill is 1.10 m wide at X19.80, Y0.00, Z20.50. The room floor extends north from the sill. The 3.00 m wide covered stone landing extends south to Z22.40 with its entire walking face at Y0.00. A 0.60 m deep stone apron spans the landing's full width and falls to Y−0.019 at Z23.00, giving a gradient of about 1:32. Both closed stone volumes retain 0.16 m thickness. The post plinths bear on the covered landing and the posts carry the existing front beam. The lean-to roof remains supported by the beam and the façade ledger.

The original georeferenced terrain tile and adaptive topology remain the underlying source. An editable formation layer sets four existing points beneath and around the entrance. The saved formation remains below the finished stone and reaches the apron toe at Y−0.019. The original cellar openings and ground outside the local blend remain in the full saved world. The runtime scene consumes the saved HCS2 terrain and the porch's closed source triangles for rendered shape, collision and foot support.

The canonical approach starts at the van's rear ramp, crosses the front garden, rises gently over the apron, continues across the covered landing and passes through the framed door into the entrance vestibule. The sheet draws the connection as a dashed guide. The porch, apron and saved ground define what players can physically stand on.

[Measured porch plan and entrance section](porch-plan-section.svg) · [Editable House scene](briar-rebuild-furnishings-flush-porch.blend) · [Reopened native verification](native-verification.json)

## Verification

The original House traversal test failed at the 40 mm step between the door and porch. The corrected doorway passes ordinary game movement in both directions, with standing and crouched bodies at the door center and 250 mm either side. The closed leaf stops arrival. The full swept audit starts in the van and reaches 15 gameplay zones and required interactions across 53,642 positions in the envelope and 50,517 in the furnished scene. ArchitecturalPlansTest also checks physical openings and acoustics through the normal House entry point. The furnishing, service and save upgrade checks pass.

The editable furnished scene reopened with all 131,805 runtime triangles and UVs, 230 packed images, the original 15 spawn anchors and all portal and socket positions. The foundation change introduces no gameplay space. The full House acceptance and release decisions remain open in the roster and stage register.
