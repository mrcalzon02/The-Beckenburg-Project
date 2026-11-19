# College native runtime review

The runtime binding opens the inspected campus GLB and companion HCS2 directly. It retains the active GLB hierarchy, original coordinates, materials and indexed surfaces. Textured materials keep their authored UVs. Plain source colours need no UVs and use a neutral texture value multiplied by the original colour factor. Dynamic equipment, weather, fog, camera, sanctuary boundaries and the canonical van still use the shared game path. The candidate is exercised through a dedicated headless review that constructs the actual HouseMap and SceneRenderer. The public College entry still resolves to the existing template.

Game movement swept all 39 flat and five vertical routes both ways. Ten physical doorway openings passed sight rays, both directions of capsule movement and room attribution. The 15-edge acoustic graph connects the authored investigation owners, and its ghost subset reaches all 14 permitted rooms. Floor and cellar ownership differs at the same horizontal coordinates. The canonical cargo ramp passes from a hunter seat to gravel and back. The closed shutter prevents movement. The three ritual reservations stand on real support. The room review covered 1,283 cross-wall samples with zero body or sight leaks at 56 same-level context seams. Exterior and vertical containment still require their own sweeps.

Standing player views were inspected on both software and the normal Windows OpenGL backend with a Tier III flashlight. The acquired images cover the cargo ramp, teaching entrance, café entrance, library entrance, north stair and cross-tunnel. Architectural dimensions and route connections read consistently. Plain blockout surfaces, the empty cross-tunnel and unfinished stair guard and service details remain visible. Installed lighting, acoustic calibration, throw and ghost checks throughout the remaining context envelope, network progression and deployment remain open.

Run from the repository root with the current engine, gameplay resources and review class on the Java classpath.

    python tools/prepare_college_runtime_review.py build/college-runtime-review.tsv
    java -ea -cp <engine-and-review-classpath> org.beckenburg.CollegeNativeRuntimeReview build/college-runtime-review.tsv build/college-runtime-software

The OpenGL review uses the normal GPU client and its pinned dependencies on the same classpath. Pass a separate output directory followed by the --gpu argument. Exact source and capture hashes, measured assertions and earlier local baseline failures are in checkpoint.json.
