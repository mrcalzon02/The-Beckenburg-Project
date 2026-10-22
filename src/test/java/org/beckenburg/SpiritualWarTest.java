package org.beckenburg;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SpiritualWarTest {
    @Test void rosterAndLimits() {
        var m = new SpiritualWar.Match();
        for (int i = 0; i < 12; i++) m.join(UUID.randomUUID(), SpiritualWar.Side.HUNTER, null);
        assertThrows(IllegalStateException.class, () -> m.join(UUID.randomUUID(), SpiritualWar.Side.HUNTER, null));
        assertThrows(IllegalArgumentException.class, () -> m.join(UUID.randomUUID(), SpiritualWar.Side.GHOST,
                GhostIdentity.Type.ELDER_NOTHIC));
    }
    @Test void wardPacksAndKillHealing() {
        var m = new SpiritualWar.Match();
        UUID h = UUID.randomUUID(), g = UUID.randomUUID();
        m.join(h, SpiritualWar.Side.HUNTER, null);
        m.join(g, SpiritualWar.Side.GHOST, GhostIdentity.Type.values()[0]);
        m.start();
        m.ward(h);
        m.ghostStrike(g, h);
        assertEquals(100, m.view(h).health());
        assertEquals(1, m.view(h).wards());
        m.ghostStrike(g, h);
        m.heal(h);
        assertEquals(100, m.view(h).health());
        assertEquals(1, m.view(h).packs());
        for(int i=0;i<4;i++)m.sword(h,g);
        assertEquals(SpiritualWar.Result.HUNTERS_WIN,m.result());
    }
    @Test void ghostRestoresHealthOnKill() {
        var m = new SpiritualWar.Match();
        UUID h=UUID.randomUUID(),g=UUID.randomUUID();
        m.join(h, SpiritualWar.Side.HUNTER,null);
        m.join(g, SpiritualWar.Side.GHOST,GhostIdentity.Type.values()[0]);
        m.start();
        m.sword(h,g);
        for(int i=0;i<5;i++)m.ghostStrike(g,h);
        assertEquals(100,m.view(g).health());
        assertEquals(SpiritualWar.Result.GHOSTS_WIN,m.result());
    }
}
