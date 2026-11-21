package castle;

/** Builds test profiles through the model API instead of exposing writable production fields. */
final class ProfileFixtures {
    static void completed(GameView view, int completed) {
        PlayerProfile old = view.profile;
        view.profile = PlayerProfile.restore(completed, old.upgradePoints(), old.upgradeLevels(),
            old.musicEnabled(), old.soundEnabled());
    }

    static void points(GameView view, float points) {
        PlayerProfile old = view.profile;
        view.profile = PlayerProfile.restore(old.completedStages(), points, old.upgradeLevels(),
            old.musicEnabled(), old.soundEnabled());
    }
}
