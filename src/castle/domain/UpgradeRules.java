package castle;

/** Shared upgrade formulas keep simulation effects and numerical descriptions consistent. */
final class UpgradeRules {
    static final int MAX_LEVEL = 5, BASE_CASTLE_GENERATION = 50;

    // Each tier adds ten percent of the base amount, rather than compounding.
    static int castleGeneration(int level) {
        return BASE_CASTLE_GENERATION + 5 * Math.max(0, Math.min(MAX_LEVEL, level));
    }

    static float attackMultiplier(int level) { return 1 + .2f * level; }
    static float defenseDivisor(int level) { return 1 + .2f * level; }
    static float movementSpeed(int kind, int level) {
        return (kind == Engine.ARCHERY ? Engine.ARCHERY_MARCH_SPEED : Engine.MARCH_SPEED) * (1 + .1f * level);
    }
    static float initialCrystals(int level) { return 200 * (1 + level * .25f); }
    static float crystalIncome(int level, int mines) { return (25 + 40 * mines) * (1 + .2f * level); }
    static float meteorDamage(int level) { return 40 * (1 + .15f * level); }
    static float stormDamage(int level) { return 50 * (1 + .15f * level); }
    static float stormDuration(int level) { return 10 * (1 + .15f * level); }
    static float mutinyDuration(int level) { return 5 * (1 + .15f * level); }
    static float spellCooldown(int spell, int level) { return new int[]{10, 8, 15, 8}[spell] / (1 + .1f * level); }

    static boolean canBuy(int level, float points) {
        return level >= 0 && level < MAX_LEVEL && points >= 1;
    }

    static int normalizeAndRefund(int[] levels) {
        int refund = 0;
        for (int i = 0; i < levels.length; i++) {
            refund += Math.max(0, levels[i] - MAX_LEVEL);
            levels[i] = Math.max(0, Math.min(MAX_LEVEL, levels[i]));
        }
        return refund;
    }
}
