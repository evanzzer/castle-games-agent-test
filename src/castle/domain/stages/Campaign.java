package castle;

/** Campaign boundaries, presentation and enemy spell policy in one place. */
enum Campaign {
    ORIGINAL(0, "I · ORIGINAL 1–30", "original", false, 1.75f, 0),
    FRONTIER(1, "II · FRONTIER 31–60", "frontier", true, 1.75f, 0),
    WILD(2, "III · WILDS 61–90", "wild", true, 1.4f, 2);

    static final int STAGES_PER_CAMPAIGN = 30;
    static final int TOTAL_STAGES = 90;
    final int page, firstStage, lastStage, enemySpellLevel;
    final String label, id;
    final boolean strategicAI;
    final float enemySpellCostMultiplier;

    Campaign(int page, String label, String id, boolean strategicAI,
             float enemySpellCostMultiplier, int enemySpellLevel) {
        this.page = page;
        this.firstStage = page * STAGES_PER_CAMPAIGN + 1;
        this.lastStage = firstStage + STAGES_PER_CAMPAIGN - 1;
        this.label = label;
        this.id = id;
        this.strategicAI = strategicAI;
        this.enemySpellCostMultiplier = enemySpellCostMultiplier;
        this.enemySpellLevel = enemySpellLevel;
    }

    static Campaign forStage(int stage) {
        if (stage < 1 || stage > TOTAL_STAGES) throw new IllegalArgumentException("Unknown stage: " + stage);
        return values()[(stage - 1) / STAGES_PER_CAMPAIGN];
    }

    static Campaign forPage(int page) {
        if (page < 0 || page >= values().length) throw new IllegalArgumentException("Unknown campaign: " + page);
        return values()[page];
    }

    boolean usesTiledBackground() { return this == ORIGINAL; }

    int localIndex(int stage) {
        if (stage < firstStage || stage > lastStage) throw new IllegalArgumentException("Stage outside campaign");
        return stage - firstStage;
    }

    int stageAt(int index) {
        if (index < 0 || index >= STAGES_PER_CAMPAIGN) throw new IllegalArgumentException("Unknown local stage");
        return firstStage + index;
    }

    String[] buildings() { return this == ORIGINAL ? Original.BUILDINGS : this == FRONTIER ? Frontier.BUILDINGS : Wilds.BUILDINGS; }
    String[] regions() { return this == ORIGINAL ? Original.MARSHES : this == FRONTIER ? Frontier.MARSHES : Wilds.MARSHES; }
    String[] altars() { return this == ORIGINAL ? Original.ALTARS : this == FRONTIER ? Frontier.ALTARS : Wilds.ALTARS; }
    String[] names() { return this == ORIGINAL ? Original.NAMES : this == FRONTIER ? Frontier.NAMES : Wilds.NAMES; }
    String[] briefs() { return this == ORIGINAL ? Original.BRIEFS : this == FRONTIER ? Frontier.BRIEFS : Wilds.BRIEFS; }
}
