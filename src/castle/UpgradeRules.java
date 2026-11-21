package castle;

final class UpgradeRules {
    static final int MAX_LEVEL = 5;

    static boolean canBuy(int level, int points) {
        return level >= 0 && level < MAX_LEVEL && points > 0;
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
