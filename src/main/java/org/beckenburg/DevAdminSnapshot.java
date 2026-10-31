package org.beckenburg;

import java.util.List;
import java.util.Objects;

/**
 * A host-local, read-only developer inspection frame. This is NOT a LAN frame,
 * player, spectator seat, or input into either team's decision maker.
 *
 * LanHost captures all values under its existing simulation mutex. Every
 * component is immutable (LanWorld.Body and LanGhostSpatial.Pose are records).
 */
public record DevAdminSnapshot(
        int tick,
        boolean running,
        List<Hunter> hunters,
        Ghost ghost,
        GhostIdentity.Type trueGhostType,
        GhostIdentity.Type hunterIdentification,
        LanCase.Outcome outcome,
        String doorSnapshot,
        String windowSnapshot,
        List<String> observedCasefile,
        List<CombatEvent> combatEvents) {

    /** Events are recorded AFTER host-authorized damage resolution, not inferred from AI commands. */
    public record CombatEvent(int tick,String seat,String action,int damage,boolean downed) { }

    public record Hunter(String seat, boolean connected, boolean ready,
                         LanWorld.Body body, int health, int stress, boolean alive) {
        public Hunter {
            Objects.requireNonNull(seat);
            Objects.requireNonNull(body);
        }
    }

    public record Ghost(boolean clientConnected, boolean ready, boolean hostAiFallback,
                        LanGhostSpatial.Pose body, int energy, boolean manifested) {
        public Ghost {
            Objects.requireNonNull(body);
        }
    }

    public DevAdminSnapshot {
        hunters = List.copyOf(hunters);
        observedCasefile = List.copyOf(observedCasefile);
        combatEvents = List.copyOf(combatEvents);
        Objects.requireNonNull(ghost);
        Objects.requireNonNull(trueGhostType);
        Objects.requireNonNull(outcome);
        Objects.requireNonNull(doorSnapshot);
        Objects.requireNonNull(windowSnapshot);
    }

    public int connectedPlayers() {
        int count = ghost.clientConnected() ? 1 : 0;
        for (Hunter hunter : hunters) if (hunter.connected()) count++;
        return count;
    }

    public long connectedHunters() {
        return hunters.stream().filter(Hunter::connected).count();
    }
}
