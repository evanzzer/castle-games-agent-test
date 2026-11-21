package castle;

import java.util.*;

public final class ApplicationCheck {
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        int[] saved = new int[11]; saved[0] = 8; saved[2] = -1;
        PlayerProfile profile = PlayerProfile.restore(0, 4, saved, true, true);
        check(profile.upgradePoints() == 7 && profile.upgradeLevel(0) == 5 && profile.upgradeLevel(2) == 0,
            "saved upgrades are normalized with the same refund policy");
        saved[0] = 0;
        int[] copy = profile.upgradeLevels(); copy[0] = 0;
        check(profile.upgradeLevel(0) == 5, "profile owns its upgrade array");
        check(profile.canEnter(1) && !profile.canEnter(2), "only first stage initially unlocked");
        profile.completeStage(1);
        check(profile.completedStages() == 1 && profile.upgradePoints() == 7.5f && profile.canEnter(2), "win advances once and awards half a point");
        profile.completeStage(1);
        check(profile.completedStages() == 1 && profile.upgradePoints() == 8, "replays reward points without advancing");
        try { profile.completeStage(3); throw new AssertionError("locked stage rewarded"); }
        catch (IllegalArgumentException expected) {}
        for (int i = 0; i < 5; i++) check(profile.purchaseUpgrade(1), "valid upgrade purchase");
        float points = profile.upgradePoints();
        check(!profile.purchaseUpgrade(1) && profile.upgradePoints() == points, "tier five cannot spend more points");
        PlayerProfile restored = PlayerProfile.restore(profile.completedStages(), profile.upgradePoints(), profile.upgradeLevels(), true, true);
        check(restored.upgradePoints() == profile.upgradePoints(), "restoring normalized data does not refund twice");
        Engine match = new MatchFactory().create(2, profile);
        check(match.attackLevels[1] == 5 && match.defenseLevels[1] == 5 && match.energy == 200, "match creation applies purchased upgrades");
        try { new MatchFactory().create(3, profile); throw new AssertionError("locked match created"); }
        catch (IllegalArgumentException expected) {}

        PlayerProfile half = PlayerProfile.restore(0, 0, new int[11], true, true);
        half.completeStage(1);
        check(half.upgradePoints() == .5f && !half.purchaseUpgrade(0), "half a point cannot buy a one-point upgrade");
        half.completeStage(2);
        check(half.upgradePoints() == 1 && half.purchaseUpgrade(0) && half.upgradePoints() == 0,
            "two stage clears fund exactly one upgrade");
        int[] experience = new int[11]; experience[3] = 4;
        PlayerProfile converted = PlayerProfile.restore(0, .5f, experience, true, true);
        check(converted.upgradePoints() == .5f && converted.upgradeLevel(3) == 4,
            "old Experience tiers become troop generation without changing balances");
        converted.completeStage(1);
        check(converted.upgradePoints() == 1 && converted.purchaseUpgrade(3), "generation tier costs one point");
        check(converted.upgradePoints() == 0 && converted.upgradeLevel(3) == 5, "generation purchase reaches tier five");
        check(new MatchFactory().create(1, converted).troopGenerationLevel == 5, "match receives generation upgrade");

        for (int tier = 0; tier <= 5; tier++) {
            int[] levels = new int[11]; levels[3] = tier;
            Engine recruitment = new MatchFactory().create(1, PlayerProfile.restore(0, 0, levels, true, true));
            recruitment.forts.clear(); recruitment.think = 100000;
            Engine.Fort own = new Engine.Fort(100, 100, 500, 1, Engine.GENERATOR); own.size = 's';
            Engine.Fort enemy = new Engine.Fort(400, 200, 500, 2, Engine.GENERATOR);
            Engine.Fort tower = new Engine.Fort(200, 100, 500, 1, Engine.ARCHERY);
            Engine.Fort mine = new Engine.Fort(300, 100, 500, 1, Engine.CRYSTAL);
            Collections.addAll(recruitment.forts, own, enemy, tower, mine);
            recruitment.tick(.625f);
            check(own.n == 500, "generation waits for the full recruitment interval");
            recruitment.tick(.625f);
            check(own.n == 550 + tier * 5 && enemy.n == 550, "all six generation tiers affect friendly castles only");
            check(tower.n == 500 && mine.n == 500, "archer and crystal buildings do not recruit");
            own.n = 995; recruitment.tick(1.25f);
            check(own.n == 1000, "upgraded generation respects capacity");
            own.n = 1125; recruitment.tick(1.25f);
            check(own.n == 1075, "overflow decay remains fifty per tick");
            own.n = 500; own.recruitTime = 1.2f;
            own.fight = new Engine.Fight(new Engine.March(enemy, own, 500)); own.fight.troops[2] = 500;
            recruitment.tick(.25f);
            check(own.n == 500 && own.recruitTime == 1.2f, "generation stays paused under siege");
        }

        List<WorldLayout.Decoration> scenery = new ArrayList<>();
        scenery.add(new WorldLayout.Decoration("tree", 1, 2, 3, 4));
        WorldLayout layout = new WorldLayout(480, 320, null, scenery); scenery.clear();
        check(layout.scenery.size() == 1, "layout owns a defensive scenery copy");
        try { layout.scenery.clear(); throw new AssertionError("mutable layout"); }
        catch (UnsupportedOperationException expected) {}

        MatchAudioEvents events = new MatchAudioEvents();
        RecordingAudio audio = new RecordingAudio();
        match.raidSerial = 2; match.audioCues.add(new Engine.AudioCue("arrow"));
        events.deliver(match, audio); events.deliver(match, audio);
        check(audio.battles == 2 && audio.cues == 1 && match.audioCues.isEmpty(), "application delivers each pending audio event once");
        Engine next = new MatchFactory().create(1, profile); next.raidSerial = 1;
        events.deliver(next, audio);
        check(audio.battles == 3, "audio event cursor resets when the match changes");
        System.out.println("Application checks passed: profile encapsulation, progression, refunds, match creation, typed layouts and one-shot event delivery.");
    }

    private static final class RecordingAudio implements GameAudio {
        int battles, cues;
        public void update(Engine e, boolean f, boolean p, boolean s, boolean m, String track, boolean ready) {}
        public void play(String file) {}
        public void play(Engine.AudioCue cue) { cues++; }
        public void playMovement() {}
        public void playBattleStart() { battles++; }
        public String chooseBattleTrack() { return "music/battle1.wav"; }
        public void pause() {}
        public void restartMusic() {}
        public void close() {}
    }
}
