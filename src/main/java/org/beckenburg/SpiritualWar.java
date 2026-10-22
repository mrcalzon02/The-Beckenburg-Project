package org.beckenburg;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Optional post-campaign arena rules. No investigation equipment, ritual,
 * evidence, campaign payouts, or permanent character progression enters here.
 * Transport/UI integration can consume this headless rules model later.
 */
public final class SpiritualWar {
    public static final int TEAM_LIMIT = 12;
    public static final int START_HEALTH = 100;
    public static final int SWORD_DAMAGE = 25;
    public static final int GHOST_STRIKE_DAMAGE = 20;
    public static final int HEALTH_PACKS_PER_HUNTER = 2;
    public static final int WARD_CHARGES_PER_HUNTER = 2;
    public static final int HEALTH_PACK_RECOVERY = 35;
    public static final int GHOST_KILL_RECOVERY = 40;

    public enum Side { HUNTER, GHOST }
    public enum Result { ACTIVE, HUNTERS_WIN, GHOSTS_WIN }
    public enum Equipment { SPIRIT_SWORD, WARD, HEALTH_PACK }

    /** Campaign completion is the sole unlock: no separate, forgeable flag. */
    public static boolean unlocked(ProfileStore profile) {
        return Objects.requireNonNull(profile).hasCampaignStory()
                && profile.storyCheckpoint() == CampaignStory.LENGTH;
    }
    public static boolean eligible(GhostIdentity.Type type) {
        return type != null && !type.elder();
    }
    public static java.util.Set<Equipment> hunterLoadout() {
        return java.util.Set.of(Equipment.SPIRIT_SWORD, Equipment.WARD, Equipment.HEALTH_PACK);
    }

    public static final class Match {
        private final Map<UUID, Fighter> fighters = new HashMap<>();
        private boolean started;
        private Result result = Result.ACTIVE;

        public record View(UUID id, Side side, GhostIdentity.Type ghost, int health,
                           int wards, int packs, boolean wardActive) {
            public boolean alive() { return health > 0; }
        }
        private static final class Fighter {
            final UUID id;
            final Side side;
            final GhostIdentity.Type ghost;
            int health = START_HEALTH;
            int wards, packs;
            boolean wardActive;
            Fighter(UUID id, Side side, GhostIdentity.Type ghost) {
                this.id = id; this.side = side; this.ghost = ghost;
                if (side == Side.HUNTER) {
                    wards = WARD_CHARGES_PER_HUNTER;
                    packs = HEALTH_PACKS_PER_HUNTER;
                }
            }
            View view() { return new View(id, side, ghost, health, wards, packs, wardActive); }
        }
        public void join(UUID id, Side side, GhostIdentity.Type ghost) {
            Objects.requireNonNull(id); Objects.requireNonNull(side);
            if (started || fighters.containsKey(id)) throw new IllegalStateException("Cannot join");
            if (side == Side.GHOST && !eligible(ghost))
                throw new IllegalArgumentException("Elder or missing ghost is ineligible");
            if (side == Side.HUNTER && ghost != null)
                throw new IllegalArgumentException("Hunters cannot select ghost types");
            if (fighters.values().stream().filter(f -> f.side == side).count() >= TEAM_LIMIT)
                throw new IllegalStateException("Team is full");
            fighters.put(id, new Fighter(id, side, ghost));
        }
        public void start() {
            if (started || fighters.values().stream().noneMatch(f -> f.side == Side.HUNTER)
                || fighters.values().stream().noneMatch(f -> f.side == Side.GHOST))
                throw new IllegalStateException("Both teams require at least one player");
            started = true;
        }
        public View view(UUID id) { return require(id).view(); }
        public Result result() { return result; }
        public boolean started() { return started; }
        private Fighter require(UUID id) {
            Fighter f = fighters.get(id);
            if (f == null) throw new IllegalArgumentException("Unknown combatant");
            return f;
        }
        private Fighter active(UUID id) {
            if (!started || result != Result.ACTIVE) throw new IllegalStateException("Match not active");
            Fighter f = require(id);
            if (f.health <= 0) throw new IllegalStateException("Eliminated combatant");
            return f;
        }
        /** Activating a ward consumes a charge; next incoming strike is negated. */
        public void ward(UUID hunter) {
            Fighter f = active(hunter);
            if (f.side != Side.HUNTER || f.wards == 0 || f.wardActive)
                throw new IllegalStateException("Ward unavailable");
            f.wards--;
            f.wardActive = true;
        }
        public void heal(UUID hunter) {
            Fighter f = active(hunter);
            if (f.side != Side.HUNTER || f.packs == 0 || f.health == START_HEALTH)
                throw new IllegalStateException("Health pack unavailable");
            f.packs--;
            f.health = Math.min(START_HEALTH, f.health + HEALTH_PACK_RECOVERY);
        }
        /** Ability damage is supplied by the existing ghost ability dispatcher. */
        public void strike(UUID attackerId, UUID targetId, int damage) {
            Fighter attacker = active(attackerId);
            Fighter target = active(targetId);
            if (attacker.side == target.side || attackerId.equals(targetId) || damage <= 0
                || damage > START_HEALTH)
                throw new IllegalArgumentException("Illegal attack");
            if (target.wardActive) {
                target.wardActive = false;
                return;
            }
            target.health = Math.max(0, target.health - damage);
            if (target.health == 0 && attacker.side == Side.GHOST)
                attacker.health = Math.min(START_HEALTH, attacker.health + GHOST_KILL_RECOVERY);
            updateResult();
        }
        public void sword(UUID hunter, UUID ghost) {
            if (require(hunter).side != Side.HUNTER) throw new IllegalArgumentException("Sword requires hunter");
            strike(hunter, ghost, SWORD_DAMAGE);
        }
        public void ghostStrike(UUID ghost, UUID hunter) {
            if (require(ghost).side != Side.GHOST) throw new IllegalArgumentException("Ghost strike requires ghost");
            strike(ghost, hunter, GHOST_STRIKE_DAMAGE);
        }
        private void updateResult() {
            boolean hunters = fighters.values().stream().anyMatch(f -> f.side == Side.HUNTER && f.health > 0);
            boolean ghosts = fighters.values().stream().anyMatch(f -> f.side == Side.GHOST && f.health > 0);
            if (!hunters) result = Result.GHOSTS_WIN;
            else if (!ghosts) result = Result.HUNTERS_WIN;
        }
    }
    private SpiritualWar() {}
}
