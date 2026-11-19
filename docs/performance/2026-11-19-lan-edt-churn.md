# LAN event dispatch and lobby frame pacing

## Root cause

Before this patch, every BODY snapshot arrived in an individual EDT task. The
lobby appended unhandled gameplay frames (BODY, GPOSE and DOORS included) to
a Swing text document even during live 3D sessions. It also constructed a new
HouseMap on every STATE and TEAMMAP update.

## Correction

- Batch at most 32 network events per EDT callback and yield to pending Swing
  input/paint callbacks. Replace only consecutive, syntactically valid BODY
  positions per seat; every other frame is a strict ordering barrier.
- Continue forwarding all incoming packets to the gameplay view. Suppress
  unsolicited raw gameplay telemetry in lobby text history while retaining
  mission notices, errors, decisions and results.
- Bound lobby text history to 240 lines and 16,384 characters.
- Cache read-only site metadata for repeated STATE and TEAMMAP handling;
  rebuild it only when the selected site changes.

## Acceptance

Run LanEdtPacketQueueTest, LanLobbyLogTest, LanLobbySiteCacheTest and the
existing full multiplayer acceptance. Tests cover exact event ordering,
malformed packets, queue fairness, private telemetry filtering, bounded
document growth and site transitions.

The change does not touch host authority, collision, evidence, ghost hearing,
network identity, gameplay clocks, geometry quality or rendering resolution.
Frame-time improvement remains to be measured on physical hardware.
