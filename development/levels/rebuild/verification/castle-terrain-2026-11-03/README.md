# Castle saved editor terrain checkpoint

Actual editor StitchedHeightmapWorld and AdaptiveTerrainTopology now persist120 punched leaves over the entire planned cellar footprint X28.5..43/Z17.5..34. TerrainFootprintCutout performs the edits; source heights remain unchanged. A second authoring run adds zero splits/cuts and produces the same SHA256 9f3d717b1ef7d5f532dcb5a66a84b02d348bbe4852bae17b351b7de993598882.

The editor-published scene exports746 faces through TerrainSceneNativePhysics. Production JAR loads the exact fingerprinted export. Castle renderer replaces the corresponding procedural ground cells with these actual faces, avoiding duplicate hidden ground. Native body and lighting queries consume the same surface. The real Editor Agent Services host exposes castle-world-terrain diagnostics through its normal TEST endpoint.

Packaged CastleWorldTerrainTest verifies every half-metre sample over the whole opening, body/optical/support passage, uncut surrounding support/collision, editor/package checksum parity, exactly746 rendered world triangles and a separate actual unpunched editor-world negative. Annex, lighting, snapshot, recovery, geometry, portal, hub, signed construction and House saved-terrain regressions pass. Native triangle inspection PNG was viewed; it is labeled native terrain inspection, not runtime cellar completion.

Next: construct actual cellar walls/floors and architectural deck void, continuous flight/landings/guards, elevation-aware furnishings and lower movement. Current architectural ground floor remains intact until that construction is integrated; this checkpoint does not claim that players can enter a completed cellar. Finish full Castle before the remaining locations; no round-time testing.
