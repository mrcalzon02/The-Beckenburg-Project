# Ravenshade School fitted foyer door

The paired canonical leaves sit in the existing 1.80 × 2.30 m fitted south frame at X22.50, Z36 and sill elevation 0. The authored jamb span is Z35.76–36.00. Each 0.90 m leaf pivots at an opposite jamb and swings south onto the supported entrance landing.

The same transformed leaf triangles feed the staged renderer, body collision and optical queries. The bound foyer portal joins forecourt owner 13 to foyer owner 0 and uses the existing shared door snapshot. Actor movement, object travel, sight and acoustic propagation follow that state. The open leaves remain solid at their hinge sides.

[Open render](../../captures/native-foyer-door-batch/school-foyer-open.png) · [Closed render](../../captures/native-foyer-door-batch/school-foyer-closed.png) · [Staged runtime proof](../verification/school-foyer-runtime.json) · [Measured arrival plan](../../measured-arrival-and-approach-pass/school/measured-plan.svg)

```mermaid
flowchart LR
    Ramp[Canonical cargo ramp] --- Gravel[Saved gravel yard]
    Gravel --- Court[Continuous forecourt]
    Court --- Landing[Founded entrance landing]
    Landing ---|Paired leaves in fitted south frame| Foyer[Pupil entrance and visitor vestibule]
```

The review uses two staged runtime maps. The released location registration, full LAN transport, other native building doors, and shared service and perimeter checks remain open.
