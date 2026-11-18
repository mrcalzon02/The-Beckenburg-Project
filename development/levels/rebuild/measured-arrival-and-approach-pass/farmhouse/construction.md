# Hollowmere Farm stair and loft approaches

The ground-floor transfer runs at X23.13 between the west well guard and the east stair guard. Their clear separation is 1.16 m. The mudroom, dining room and cleaner use their existing 1.05 m fitted doors. The route turns on full landings at both ends of the well.

The barn loft approach crosses the actual X39 threshold at Z17.50, then turns north in the 1.09 m aisle between the wall and the flight guard. The house, basement, loft and their terrain keep their saved structural assemblies.

[Measured plan](measured-plan.svg) · [Physical export review](export-review.json) · [Joined native walks](../verification/journeys-and-ramp-runtime.json)

```mermaid
flowchart LR
    Van[Canonical van ramp] --- Yard[Farmyard]
    Yard ---|Mudroom door| Mud[Mudroom]
    Mud ---|South stair door| Transfer[Ground stair transfer]
    Dining[Dining] ---|West stair door| Transfer
    Transfer ---|North cleaner door| Cleaner[Cleaner store]
    Cellar[Cellar passage] ---|Cellar door and flight| Transfer
    Transfer ---|Upper flight| Landing[Upper landing]
    Yard ---|Barn entrance| Barn[Barn aisle]
    Barn ---|Loft stair door| LoftStair[Full loft stair landing]
    LoftStair ---|Barn flight| Loft[Hayloft]
```

The plan's dashed guides indicate movement through the supported floors and real frames. Shared investigation service and multiplayer evidence remains open.
