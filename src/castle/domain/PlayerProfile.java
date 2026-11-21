package castle;

/** Owns player progression, upgrade purchases and settings independently of save storage. */
final class PlayerProfile {
    static final int UPGRADE_COUNT = 11;
    private int completed, points;
    private final int[] upgrades;
    private boolean musicEnabled, soundEnabled;

    private PlayerProfile(int completed, int points, int[] levels, boolean music, boolean sound) {
        if (levels.length != UPGRADE_COUNT) throw new IllegalArgumentException("Expected eleven upgrades");
        this.completed = Math.max(0, Math.min(Campaign.TOTAL_STAGES, completed));
        this.upgrades = levels.clone();
        this.points = points + UpgradeRules.normalizeAndRefund(this.upgrades);
        musicEnabled = music;
        soundEnabled = sound;
    }

    static PlayerProfile restore(int completed, int points, int[] upgrades, boolean music, boolean sound) {
        return new PlayerProfile(completed, points, upgrades, music, sound);
    }

    int completedStages() { return completed; }
    int upgradePoints() { return points; }
    int upgradeCount() { return upgrades.length; }
    int upgradeLevel(int index) { return upgrades[index]; }
    int[] upgradeLevels() { return upgrades.clone(); }
    boolean musicEnabled() { return musicEnabled; }
    boolean soundEnabled() { return soundEnabled; }
    void toggleMusic() { musicEnabled = !musicEnabled; }
    void toggleSound() { soundEnabled = !soundEnabled; }
    void setSoundEnabled(boolean enabled) { soundEnabled = enabled; }
    int nextStage() { return Math.min(Campaign.TOTAL_STAGES, completed + 1); }

    boolean canEnter(int stage) {
        return stage >= 1 && stage <= Campaign.TOTAL_STAGES && stage <= completed + 1;
    }

    boolean canUpgrade(int index) {
        return UpgradeRules.canBuy(upgrades[index], points);
    }

    boolean purchaseUpgrade(int index) {
        if (!canUpgrade(index)) return false;
        points--;
        upgrades[index]++;
        return true;
    }

    // Replays earn points, but only the next uncleared stage advances progression.
    void completeStage(int stage) {
        if (!canEnter(stage)) throw new IllegalArgumentException("Cannot reward a locked stage");
        if (stage == completed + 1) completed = stage;
        points += 2 + upgrades[3] / 3;
    }
}
