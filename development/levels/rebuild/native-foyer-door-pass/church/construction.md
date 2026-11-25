# St. Brigid Parish fitted approaches

The chapel approach crosses the 1.05 m crypt entrance at X28.50, Z26 and continues on the full floor west of the well. The west guard spans X30.45–30.53. The final turn runs parallel to the guard before joining the existing stair landing.

The vestry, consultation and confessional approaches align with their physical apertures before turning. The choir, archive and crypt links use their installed frame widths. The current wall, floor, well and stair models remain the physical source.

[Measured crypt plan](measured-plan.svg) · [Crypt threshold export review](export-review.json) · [Joined native walks](../verification/joined-review.json)

```mermaid
flowchart LR
    Chapel[Chapel] ---|South fitted frame| Landing[West crypt landing]
    Landing ---|Guarded descending flight| Crypt[Crypt stair]
    Crypt ---|Lower fitted door| Aisle[Crypt aisle]
    Aisle --- Memorial[Memorial]
    Nave[Rear nave] ---|Choir flight| Gallery[Upper stair landing]
    Gallery ---|Choir frame| Choir[Choir]
    Choir ---|Music entry frame| Music[Music entry]
    Music ---|Archive frame| Archive[Music archive]
```
