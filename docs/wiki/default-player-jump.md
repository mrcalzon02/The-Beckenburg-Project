# Default player jump

Every `EditorProject` exposes `createPlayerMotion()` returning the standalone Java 17 `DefaultPlayerMotion` controller. It is included by both editor-kit build scripts without game dependencies.

Supply a `Collision` adapter with capsule clearance and the highest support crossed on descent. Call `jump(x,y,z,vx,vz,height,collision)` only with grounded feet and authoritative ground movement velocity in metres/second. Advance the controller with elapsed seconds, at most .25 per call; it substeps at .01 seconds. `active()` includes the brief landing crouch; `airborne()` is the flight phase. While active, render the existing crouch sprite at its x/y/z. When finished, stand if the capsule fits and the player has not requested crouching. Reset the controller at spawn/scene changes.

Defaults: .73 m tuck at takeoff, 4.8 m/s vertical velocity, 18 m/s² gravity and .12 seconds landing recovery. Horizontal velocity is carried from takeoff, including running; repeated jump and extra air input do not add velocity. Ground movement and standing/crouching remain adapter responsibilities.

Beckenburg investigations connect this controller to the shared solo/co-op `LanWorld` host, `JUMP` client intent and existing `BODY`/`POSTURE` replication. Space jumps, Shift runs, Ctrl crouches. The host owns gravity and collision even after input stops. Castle currently provides walking/running/crouching with the updated keys; its separate collision adapter still needs jump adoption.
