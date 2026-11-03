# Van fit verification

Source checkpoint: `210e3da2b3101a96f6399b1c9ec6338b6382c126`, on `map/first-principles-rebuild-plan`, preserving concurrent hunter checkpoint `c876e1635604c758c5b8af9d55821f5a4fe118c6`.

The exact committed source was exported using `git archive`, independently compiled with Java 17 (all main and test sources), then tested using packaged runtime assets and isolated test homes/preferences. All 13 checks in `results.json` pass. Real loopback host/guest rack replication ran outside the network-restricted sandbox. Actual logs are retained here. Both newly added regressions fail against baseline production classes; see `baseline-negative-control.log`.

The 42 original before/after PNGs and detailed fit review are in `development/levels/previews/van-unification/`. Gallery generation/check pass at 2,283 original PNGs. Original donor OBJ UV corners and source/atlas hashes are preserved in `donor-rear-source.json`. The original runtime GLB remains byte-identical (SHA-256 `1E336CE716314A7BFB182C33A58F3ECEEB73CB4C4740815B918006A72D2C3370`).

This checkpoint verifies the shared van geometry, donor door texture mapping, tyre cavities, stock packing, collision/lighting, all-site station access and focused network behavior. It does not mark unrelated recovery gates, installed GPU acceptance or a new installer complete. The gallery is committed with the branch; deployment of the public main-branch Pages site is separate.
