package castle;

/** Campaign boundaries, presentation and enemy spell policy in one place. */
enum Campaign {
    ORIGINAL(0, "I · ORIGINAL 1–30", "original", false, 1.75f, 0, 0),
    FRONTIER(1, "II · FRONTIER 31–60", "frontier", true, 1.75f, 0, 0),
    WILD(2, "III · WILDS 61–90", "wild", true, 1.4f, 2, 0),
    CITADEL(3, "IV · CITADEL 91–120", "citadel", true, 1.25f, 3, 3),
    ZENITH(4, "V · ZENITH 121–150", "zenith", true, 1.1f, 4, 4);

    static final int STAGES_PER_CAMPAIGN = 30;
    static final int TOTAL_STAGES = 150;
    final int page, firstStage, lastStage, enemySpellLevel, enemyUpgradeTier;
    final String label, id;
    final boolean strategicAI;
    final float enemySpellCostMultiplier;

    Campaign(int page, String label, String id, boolean strategicAI,
             float enemySpellCostMultiplier, int enemySpellLevel, int enemyUpgradeTier) {
        this.page = page;
        this.firstStage = page * STAGES_PER_CAMPAIGN + 1;
        this.lastStage = firstStage + STAGES_PER_CAMPAIGN - 1;
        this.label = label;
        this.id = id;
        this.strategicAI = strategicAI;
        this.enemySpellCostMultiplier = enemySpellCostMultiplier;
        this.enemySpellLevel = enemySpellLevel;
        this.enemyUpgradeTier = enemyUpgradeTier;
    }

    static Campaign forStage(int stage) {
        if (stage < 1 || stage > TOTAL_STAGES) throw new IllegalArgumentException("Unknown stage: " + stage);
        return values()[(stage - 1) / STAGES_PER_CAMPAIGN];
    }

    static Campaign forPage(int page) {
        if (page < 0 || page >= values().length) throw new IllegalArgumentException("Unknown campaign: " + page);
        return values()[page];
    }

    // Frontier and Wild advance combat tiers halfway through their thirty-stage campaigns.
    int enemyAttackLevel(int stage) {
        int half = localIndex(stage) / 15;
        switch (this) {
            case FRONTIER: return 1 + half;
            case WILD: return 3 + half;
            case CITADEL: case ZENITH: return 5;
            default: return 0;
        }
    }

    int enemyDefenseLevel(int stage) { return this == ZENITH ? 5 : Math.max(0, enemyAttackLevel(stage) - 1); }

    boolean usesTiledBackground() { return this == ORIGINAL; }

    int localIndex(int stage) {
        if (stage < firstStage || stage > lastStage) throw new IllegalArgumentException("Stage outside campaign");
        return stage - firstStage;
    }

    int stageAt(int index) {
        if (index < 0 || index >= STAGES_PER_CAMPAIGN) throw new IllegalArgumentException("Unknown local stage");
        return firstStage + index;
    }

    String[] buildings() { return this == ORIGINAL ? Original.BUILDINGS : this == FRONTIER ? Frontier.BUILDINGS : this == WILD ? Wilds.BUILDINGS : this == CITADEL ? Citadel.BUILDINGS : Zenith.BUILDINGS; }
    String[] regions() { return this == ORIGINAL ? Original.MARSHES : this == FRONTIER ? Frontier.MARSHES : this == WILD ? Wilds.MARSHES : this == CITADEL ? Citadel.MARSHES : Zenith.MARSHES; }
    String[] altars() { return this == ORIGINAL ? Original.ALTARS : this == FRONTIER ? Frontier.ALTARS : this == WILD ? Wilds.ALTARS : this == CITADEL ? Citadel.ALTARS : Zenith.ALTARS; }
    String[] names() { return this == ORIGINAL ? Original.NAMES : this == FRONTIER ? Frontier.NAMES : this == WILD ? Wilds.NAMES : this == CITADEL ? Citadel.NAMES : Zenith.NAMES; }
    String[] briefs() { return this == ORIGINAL ? Original.BRIEFS : this == FRONTIER ? Frontier.BRIEFS : this == WILD ? Wilds.BRIEFS : this == CITADEL ? Citadel.BRIEFS : Zenith.BRIEFS; }
}
