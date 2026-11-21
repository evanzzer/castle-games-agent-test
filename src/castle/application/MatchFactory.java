package castle;

/** Application use case that starts an unlocked stage with the player's purchased upgrades. */
final class MatchFactory {
    Engine create(int stage, PlayerProfile profile) {
        if (!profile.canEnter(stage)) throw new IllegalArgumentException("Stage is locked");
        Engine engine = new Engine(stage, 0);
        engine.attackLevels[1] = profile.upgradeLevel(0);
        engine.defenseLevels[1] = profile.upgradeLevel(1);
        engine.speedLevel = profile.upgradeLevel(2);
        engine.crystalLevel = profile.upgradeLevel(5);
        engine.cooldownLevel = profile.upgradeLevel(10);
        engine.energy = 200 * (1 + profile.upgradeLevel(4) * .25f);
        for (int spell = 0; spell < 4; spell++) engine.skillLevels[spell] = profile.upgradeLevel(6 + spell);
        return engine;
    }
}
