package org.beckenburg;

import java.util.UUID;

/** Standalone rules regression, consistent with the project's main-method test runner. */
public final class SpiritualWarTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static void reject(Runnable action) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException expected) {
            rejected = true;
        }
        check(rejected, "Expected invalid action to be rejected");
    }
    private static GhostIdentity.Type standardGhost() {
        for (var type : GhostIdentity.Type.values())
            if (!type.elder() && !type.ancient()) return type;
        throw new AssertionError("No standard ghost");
    }
    public static void main(String[] args) {
        var unstarted = new SpiritualWar.Match();
        reject(unstarted::start);
        check(!SpiritualWar.unlocked(new ProfileStore(java.nio.file.Path.of("unused.properties"))),
            "Fresh campaign cannot enter Spiritual War");
        for (int i = 0; i < 12; i++)
            unstarted.join(UUID.randomUUID(), SpiritualWar.Side.HUNTER, null);
        reject(() -> unstarted.join(UUID.randomUUID(), SpiritualWar.Side.HUNTER, null));
        reject(() -> unstarted.join(UUID.randomUUID(), SpiritualWar.Side.GHOST,
            GhostIdentity.Type.ELDER_NOTHIC));
        check(SpiritualWar.hunterLoadout().size() == 3, "Exactly three permitted tools");

        var match = new SpiritualWar.Match();
        UUID hunter = UUID.randomUUID(), ghost = UUID.randomUUID();
        match.join(hunter, SpiritualWar.Side.HUNTER, null);
        match.join(ghost, SpiritualWar.Side.GHOST, standardGhost());
        match.start();
        match.ward(hunter);
        match.ghostStrike(ghost, hunter);
        check(match.view(hunter).health() == 100 && match.view(hunter).wards() == 1,
            "Ward absorbs one strike and costs one charge");
        match.ghostStrike(ghost, hunter);
        match.heal(hunter);
        check(match.view(hunter).health() == 100 && match.view(hunter).packs() == 1,
            "Pack heals without overheal and is consumed");
        for (int i = 0; i < 4; i++) match.sword(hunter, ghost);
        check(match.result() == SpiritualWar.Result.HUNTERS_WIN, "Hunter eliminated last ghost");
        reject(() -> match.ghostStrike(ghost, hunter));

        var rematch = new SpiritualWar.Match();
        UUID h = UUID.randomUUID(), g = UUID.randomUUID();
        rematch.join(h, SpiritualWar.Side.HUNTER, null);
        rematch.join(g, SpiritualWar.Side.GHOST, standardGhost());
        rematch.start();
        rematch.sword(h, g);
        for (int i = 0; i < 5; i++) rematch.ghostStrike(g, h);
        check(rematch.view(g).health() == 100 && rematch.result() == SpiritualWar.Result.GHOSTS_WIN,
            "Confirmed ghost kill heals to cap and concludes round");
        var timed = new SpiritualWar.Match();
        UUID th = UUID.randomUUID(), tg = UUID.randomUUID();
        timed.join(th, SpiritualWar.Side.HUNTER, null);
        timed.join(tg, SpiritualWar.Side.GHOST, standardGhost());
        reject(() -> timed.advanceTime(1));
        timed.start();
        var visual = new SceneRenderer();
        visual.syncSpiritualWarSanctuary(timed);
        check(visual.spiritualWarVanSanctuaryVisible(), "Blue van boundary enabled at match start");
        reject(() -> timed.advanceTime(-1));
        check(timed.vanProtected(), "Van starts protected");
        timed.advanceTime(900);
        check(!timed.vanProtected(), "Van unprotected at fifteen minutes");
        visual.syncSpiritualWarSanctuary(timed);
        check(!visual.spiritualWarVanSanctuaryVisible(), "Blue boundary removed with sanctuary");
        timed.advanceTime(899);
        check(timed.remainingSeconds() == 1 && timed.result() == SpiritualWar.Result.ACTIVE,
            "Still active with one second remaining");
        timed.advanceTime(1);
        check(timed.remainingSeconds() == 0 && timed.result() == SpiritualWar.Result.TIME_LIMIT_DRAW,
            "Exactly thirty minutes forces a draw with survivors");
        reject(() -> timed.sword(th, tg));
        timed.advanceTime(100);
        check(timed.result() == SpiritualWar.Result.TIME_LIMIT_DRAW, "Terminal state persists");
        var scoring = new SpiritualWar.Match();
        UUID sh=UUID.randomUUID(), sg=UUID.randomUUID();
        scoring.join(sh,SpiritualWar.Side.HUNTER,null);
        scoring.join(sg,SpiritualWar.Side.GHOST,standardGhost());
        scoring.start();
        scoring.sword(sh,sg);
        scoring.advanceTime(1800);
        check(scoring.result()==SpiritualWar.Result.HUNTERS_WIN, "Damage tiebreak");
        var headcount = new SpiritualWar.Match();
        headcount.join(UUID.randomUUID(),SpiritualWar.Side.HUNTER,null);
        headcount.join(UUID.randomUUID(),SpiritualWar.Side.HUNTER,null);
        headcount.join(UUID.randomUUID(),SpiritualWar.Side.GHOST,standardGhost());
        headcount.start();
        headcount.advanceTime(1800);
        check(headcount.result()==SpiritualWar.Result.HUNTERS_WIN, "Survivor advantage");
        System.out.println("PASS Spiritual War headless roster, wards, healing and elimination");
    }
}
